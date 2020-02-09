import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.Source
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object Collector extends App {
  private object Tick
  val config = ConfigFactory.load()
  val redisConf = config.getConfig("redis")
  val collectorConfig = config.getConfig("collector")
  val latency = collectorConfig.getInt("latencyInHours")
  val redis = new RedisClient(redisConf.getString("host"), redisConf.getInt("port"))
  implicit val system = ActorSystem("collector")

  Source
    .tick(1.second, 1.second, Tick)
    .runForeach(_ => aggregate())

  private def aggregate() = {
    val hours = redis
      .keys("chunk:*")
      .map(_.flatten)
      .getOrElse(Nil)
      .map(key => key.drop("chunk:".length).takeWhile(_ != ':') -> key)
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .filter { h =>
        val deleted = redis.exists(s"deleted:${h._1}")
        if (deleted) h._2.foreach(k => redis.del(k))
        !deleted
      }

    hours.foreach { h =>
      val sum = h._2.flatMap(k => redis.scard(k)).sum
      redis.set(s"uniqs:${h._1}", sum)
    }

    val curHour = DateTime.now.clicks / (60 * 60 * 1000)

    hours
      .filter(_._1.toLong + latency < curHour)
      .foreach{ h =>
        redis.set(s"deleted:${h._1}", "")
        h._2.foreach(k => redis.del(k))
      }
  }
}
