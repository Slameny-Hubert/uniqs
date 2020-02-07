package rule.cache

trait CacheBase {
  protected def getId(l: String) = l.drop(2).toInt
}
