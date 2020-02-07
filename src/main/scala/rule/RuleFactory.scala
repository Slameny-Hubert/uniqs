package rule

import akka.actor.ActorSystem
import com.typesafe.config.Config
import rule.cache.{MovieCache, NameCache}

import scala.concurrent.ExecutionContext

class RuleFactory(conf: Config)(implicit ec: ExecutionContext, system: ActorSystem) {
  implicit val nameCache = new NameCache(conf)
  implicit val movieCache = new MovieCache(conf, nameCache)
  val movieSearch = new MovieSearchRule
  val nameSearch = new NameSearchRule
}
