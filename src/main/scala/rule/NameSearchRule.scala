package rule

import rule.cache.{MovieCache, NameCache}

import scala.concurrent.{ExecutionContext, Future}

class NameSearchRule(implicit movieCache: MovieCache, nameCache: NameCache, ec: ExecutionContext) extends RuleBase {
  def byName(title: String): Future[Seq[response.NameResponse]] = Future{
    nameCache.getByName(title).map(response.NameResponse.apply).sortBy(_.id)
  }
}
