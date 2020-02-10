package com.mgorokhovsky.service

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.scaladsl.model.DateTime
import com.mgorokhovsky.service.Accumulator.{Action, Click, Event, Impression}
import com.redis._
import com.typesafe.config.Config
import scala.collection.mutable

/***
 * Buffer. Accumulates the data and sends it to Redis
 */
class Accumulator(conf: Config, redis: RedisClient) extends Actor with ActorLogging {
  private val millisInHour = 60 * 60 * 1000

  // data for 1 hour
  private class HourlyData(chunksNumber: Int, hourStamp: Long) {
    // chunk of uids which have the same hash
    // keeping uids in different chunks gives us an ability to shard the data on Redis side
    private class Chunk(id: Int) {
      private var clicksCounter = 0L
      private var impressionCounter = 0L
      private var uids = mutable.Set.empty[String]
      // the last flush time
      private var flushed = 0L

      // add an action to the chunk
      def add(uid: String, act: Action, hs: Long) = {
        uids += uid

        act match {
          case Click => clicksCounter += 1
          case Impression => impressionCounter += 1
        }
        // if the limit is exceeded we must flush the data
        if (clicksCounter + impressionCounter >= conf.getInt("maxChunkSize"))
          flush()
      }

      // send chunk to Redis and delete the data
      private def flush() =
        if (uids.nonEmpty) {
          redis.sadd(s"chunk:$hourStamp:$id", uids.head, uids.tail.toList:_*)
          redis.incrby(s"clicks:$hourStamp", clicksCounter)
          redis.incrby(s"impressions:$hourStamp", impressionCounter)
          uids.clear()
          impressionCounter = 0
          clicksCounter = 0
          // remember the flush time
          flushed = DateTime.now.clicks
          log.debug(s"Flushed: $hourStamp, $id")
        }

      // if we have not been flushing too long because of the low traffic
      // we can update the data on Redis anyway
      def forcedFlush() =
        if (DateTime.now.clicks - flushed > 1000)
          flush()
    }

    private val chunks = Array.tabulate(chunksNumber)(new Chunk(_))

    // add an action to the hour
    def add(uid: String, act: Action) = {
      val id = uid.hashCode.abs % chunksNumber
      chunks(id).add(uid, act, hourStamp)
    }

    def forcedFlush() = chunks.foreach(_.forcedFlush())
  }

  private val chunksNumber = conf.getInt("chunksNumber")
  private val hours = mutable.Map.empty[Long, HourlyData]

  override def receive: Receive = {
    case Event(uid, ts, action) =>
      log.debug(s"Got $uid, $ts, $action")
      val hourStamp = ts / millisInHour

      hours
        .getOrElseUpdate(hourStamp, new HourlyData(chunksNumber, hourStamp))
        .add(uid, action)

    case Tick => hours.foreach(_._2.forcedFlush())
  }

  def forcedFlush() = hours.foreach(_._2.forcedFlush())
}

object Accumulator {
  def props(conf: Config, redis: RedisClient): Props = Props(new Accumulator(conf, redis))

  sealed class Action

  object Click extends Action {
    override def toString: String = "click"
  }

  object Impression extends Action {
    override def toString: String = "impression"
  }

  case class Event(uid: String, ts: Long, action: Action)
}