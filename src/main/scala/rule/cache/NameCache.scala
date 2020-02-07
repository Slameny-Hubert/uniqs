package rule.cache

import akka.actor.ActorSystem
import com.typesafe.config.Config
import db.Tsv
import model.Name

import scala.collection.mutable

class NameCache(conf: Config)(implicit system: ActorSystem) extends CacheBase {
  private type ByTitle = mutable.Map[Int, Name]
  private var idMap = new mutable.HashMap[Int, Name]()
  private var nameMap = new mutable.HashMap[String, Seq[Name]]()

  def getById(id: Int): Option[Name] = idMap.get(id)
  def getByName(name: String): Seq[Name] = nameMap.getOrElse(name.toLowerCase, Seq.empty)

  Tsv(conf.getString("paths.nameBasics"), (_: Seq[String], l: Seq[String]) => {
    val id = getId(l.head)
    val name = Name(id, l(1))
    idMap += id -> name
    val key = l(1).toLowerCase
    nameMap += key -> (nameMap.getOrElse(key, Nil) :+ name)
  })
}
