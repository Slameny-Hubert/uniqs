package response

import rule.cache.MovieCache

case class NameResponse(id: String,	primaryName: String, typecast: Seq[String], baconDistance: Option[Int])

object NameResponse {
  def apply(n: model.Name)(implicit movieCache: MovieCache): NameResponse = {
    val typecast = n
      .movies
      .flatMap(m => movieCache.getGenres.keys.filter(g => (m.genres & g) > 0))
      .groupBy(identity)
      .filter(_._2.length >= (n.movies.length+1)/2)
      .keys
      .flatMap(movieCache.getGenres.get)
      .toList
      .sorted

    val baconDistance = Some(n.baconDistance).filter(0<=)

    NameResponse("nm%07d".format(n.id), n.primaryName, typecast, baconDistance)
  }
}

