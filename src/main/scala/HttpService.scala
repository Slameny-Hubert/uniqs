import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.redis.RedisClient
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor

trait Service {
  import Accumulator._
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit def config: Config
  val accumulator: ActorRef
  val redis: RedisClient

  def getStat(ts: Long) = {
    val hs = ts / (60 * 60 * 1000)
    val uniqs = redis.get(s"uniqs:$hs").map(_.toInt).getOrElse(0)
    val clicks = redis.get(s"clicks:$hs").map(_.toInt).getOrElse(0)
    val impressions = redis.get(s"impressions:$hs").map(_.toInt).getOrElse(0)
    (uniqs, clicks, impressions)
  }

  val route: Route = {
    path("analytics") {
      post {
        parameters('timestamp.as[Long], 'user.as[String], 'click.as[String]) { (ts, user, _) =>
          accumulator ! Event(user, ts, Click)
          complete(StatusCodes.OK)
        } ~
          parameters('timestamp.as[Long], 'user.as[String], 'impression.as[String]) { (ts, user, _) =>
            accumulator ! Event(user, ts, Impression)
            complete(StatusCodes.OK)
          }
      } ~
        get {
          parameters('timestamp.as[Long]) { ts =>
            val (uniqs, clicks, impressions) = getStat(ts)
            complete(HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              s"unique_users,$uniqs\nclicks,$clicks\nimpressions,$impressions"
            ))
          }
        }
    }
  }
}

object HttpService extends App with Service {
  override implicit val system = ActorSystem("uniqs")
  override implicit val executor = system.dispatcher
  override val config = ConfigFactory.load()
  val redisConf = config.getConfig("redis")
  override val redis = new RedisClient(redisConf.getString("host"), redisConf.getInt("port"))
  override val accumulator = system.actorOf(Accumulator.props(config.getConfig("accumulator"), redis))
  val bindingFuture = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))

  println(s"Server online at http://${config.getString("http.interface")}:${config.getInt("http.port")}/analytics")
}
