package rule

import response.MovieResponse
import rule.cache.{MovieCache, NameCache}

import scala.concurrent.{ExecutionContext, Future}

class MovieSearchRule(implicit movieCache: MovieCache, nameCache: NameCache, ec: ExecutionContext) extends RuleBase {
  def byTitle(title: String): Future[Seq[response.MovieResponse]] = Future{
    movieCache.getByTitle(title).map(response.MovieResponse.apply)
  }

  /** returns top movies
    * @param genre - genre name
    * @param qnt - quantity of movies to return
    * @param offset - offset
    * @return None - if there is no such genre, or seq of movies with ratings
    */
  def byGenre(genre: String, qnt: Int, offset: Int): Future[Option[Seq[response.TopResponse]]] = Future{
    movieCache
      .getGenreByName(genre.toLowerCase)
      .map(id => movieCache.getTop( id, qnt, offset ).map( response.TopResponse.apply ))
  }

  def together(names: Set[String]): Future[Seq[MovieResponse]] = Future{
    names
      .map(nameCache.getByName(_).flatMap(_.movies).toSet)
      .reduce(_ intersect _)
      .map(response.MovieResponse.apply)
      .toSeq
  }
}
