package rule.cache

import scala.collection.mutable
import akka.actor.ActorSystem
import com.typesafe.config.Config

import db.Tsv
import model._

class MovieCache(conf: Config, nameCache: NameCache)(implicit system: ActorSystem) extends CacheBase {
  private type ByTitle = mutable.Map[String, List[Movie]]
  private var titleMap = new mutable.HashMap[String, List[Movie]]()
  private var idMap = new mutable.HashMap[Int, Movie]
  private val genres = new mutable.HashMap[String, Int]
  private lazy val genresById = genres.map{case (n, id) => (id, n)}.toMap
  private val genreTops = new mutable.HashMap[Int, Vector[Top]]

  // read title.basics and populate it into 2 HashMaps: by id and by name (prime and original)
  Tsv(conf.getString("paths.titleBasics"), (_: Seq[String], l: Seq[String]) => {
    val isAdult = l(4) match {
      case "0" => false
      case "1" => true
    }

    val id = getId(l.head)
    val genre = l(8)
      .split(",")
      .filter("\\N"!=)
      .map(g => genres.getOrElseUpdate(g.toLowerCase, 1 << genres.size))
      .reduceOption(_ | _)
      .getOrElse(0)

    val movie = Movie(id, /*l(1),*/ l(2), l(3), isAdult, optNum(l(5)), optNum(l(6)), optNum(l(7)), genre)
    val keyPrim = l(2).toLowerCase
    val keyOrig = l(3).toLowerCase
    idMap += id -> movie
    titleMap += keyPrim -> (titleMap.getOrElse(keyPrim, Nil) :+ movie)
    if (keyPrim != keyOrig) {
      titleMap += keyOrig -> (titleMap.getOrElse(keyOrig, Nil) :+ movie)
    }
  })

  // read title.principals. Every pair movie-name is populated into movies and into names caches
  Tsv(
    conf.getString("paths.titlePrincipals"),
    (_: Seq[String], l: Seq[String]) => nameCache.getById(getId(l(2))).foreach{n =>
        val m = idMap.get(getId(l(0)))
        m.foreach({ m =>
          m.principals += n
          n.movies += m
        })}
  )

  // read movies ratings
  {
    val tops = new mutable.HashMap[Int, mutable.SortedSet[Top]]

    Tsv(
      conf.getString("paths.titleRatings"),
      (_: Seq[String], l: Seq[String]) => idMap
        .get(getId(l(0)))
        .foreach{m => genresById
          .keys
          .filter(g => (g & m.genres) > 0)
          .foreach(g => tops
            .getOrElseUpdate(g, mutable.SortedSet[Top]()(Ordering.by[Top, Int](-_.rate)))
              += Top(m, l(1).filter('.'!=).toInt)
          )
        }
    )

    tops.foreach(g => genreTops(g._1) = g._2.toVector)
  }

  // calculate Kevin Bacon distance
  private val kevinBacon = nameCache.getByName("Kevin Bacon").head
  baconDistance(Set(kevinBacon), 0)

  def getByTitle(title: String): Seq[Movie] = titleMap.getOrElse(title.toLowerCase, Seq.empty[Movie])
  def getById(id: Int): Option[Movie] = idMap.get(id)
  def getGenres = genresById
  def getGenreByName(g: String) = genres.get(g.toLowerCase)

  def getTop(genreId: Int, quantity: Int, offset: Int) =
    genreTops(genreId).slice(offset, offset + quantity)

  private def optNum(s: String) = {
    val yearRe = "(\\d+)".r

    s match {
      case yearRe(v) => Some(v.toInt)
      case "\\N" => None
    }
  }

  private def baconDistance(names: Set[Name], gen: Int): Unit = {
    system.log.debug(s"Kevin Bacon distance: generation $gen of size ${names.size}")
    names.foreach(_.baconDistance = gen)
    val children = names.flatMap(_.movies.flatMap(_.principals.filter(_.baconDistance == -1)))
    if(children.nonEmpty)
      baconDistance(children, gen + 1)
  }
}
