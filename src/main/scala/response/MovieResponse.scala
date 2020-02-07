package response

import rule.cache.MovieCache

case class MovieResponse(
  tconst: String,
  primaryTitle: String,
  originalTitle: String,
  isAdult: Boolean,
  startYear: Option[Int],
  endYear: Option[Int],
  runtimeMinutes: Option[Int],
  genres: String,
  principals: Seq[NameResponse]
)

object MovieResponse {
  def apply(m: model.Movie)(implicit movieCache: MovieCache): MovieResponse = MovieResponse(
    "tt%07d".format(m.id),
    m.primaryTitle,
    m.originalTitle,
    m.isAdult,
    m.startYear,
    m.endYear,
    m.runtimeMinutes,
    movieCache.getGenres.filterKeys(g => (g & m.genres) > 0).values.toList.sorted.mkString(","),
    m.principals.map(NameResponse.apply)
  )
}
