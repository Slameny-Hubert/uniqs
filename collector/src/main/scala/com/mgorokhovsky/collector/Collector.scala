package com.mgorokhovsky.collector

import akka.actor.ActorSystem
import akka.http.scaladsl.model.DateTime
import akka.stream.scaladsl.Source
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

/***
  Job to aggregate sets of uniq uids,
 it reads chunks of uids `chunk:<hour>:<id>` and summarises their quantity to `uniqs:<hour>`
 also it deletes chunks older than `latencyInHours`
 */

object Collector extends App {
  private object Tick
  val config = ConfigFactory.load()
  val redisConf = config.getConfig("redis")
  val collectorConfig = config.getConfig("collector")
  val latency = collectorConfig.getInt("latencyInHours")
  val redis = new RedisClient(redisConf.getString("host"), redisConf.getInt("port"))
  implicit val system = ActorSystem("collector")

  // run the aggregation every second
  Source
    .tick(1.second, 1.second, Tick)
    .runForeach(_ => aggregate())

  // main method
  private def aggregate() = {
    val hours = redis
      .keys("chunk:*") // get all existing chunks
      .map(_.flatten)
      .getOrElse(Nil)
      .map(key => key.drop("chunk:".length).takeWhile(_ != ':') -> key) // extract the hour from the key name
      .groupBy(_._1) // group by hours
      .mapValues(_.map(_._2))
      .filter { h => // filter out zombie chunks
        // if the hour has been deleted, but we got events for that hour,
        // chunks were recreated, but we can not process them correctly
        val deleted = redis.exists(s"deleted:${h._1}") // we have a special key for every deleted hour `deleted:<hour>`
        if (deleted) h._2.foreach(k => redis.del(k)) // if we have found the key - it is a zombie
        !deleted
      }

    // aggregate the rest of chunks
    hours.foreach { h =>
      val sum = h._2.flatMap(k => redis.scard(k)).sum
      redis.set(s"uniqs:${h._1}", sum)
    }

    // current hour from begin of the epoch
    val curHour = DateTime.now.clicks / (60 * 60 * 1000)

    // old chunks can be deleted
    hours
      .filter(_._1.toLong + latency < curHour)
      .foreach{ h =>
        // add the key for deleted hours
        // we need them to distinguish zombie chunks from old chunks which were not processed due to technical problems
        redis.set(s"deleted:${h._1}", "")
        h._2.foreach(k => redis.del(k))
      }
  }
}
