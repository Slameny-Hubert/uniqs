package response

import rule.cache.MovieCache

case class TopResponse(movie: MovieResponse,	rate: String)

object TopResponse {
  def apply(t: model.Top)(implicit movieCache: MovieCache): TopResponse = TopResponse(
    MovieResponse(t.movie),
    (t.rate / 10).toString + '.' + (t.rate % 10)
  )
}




