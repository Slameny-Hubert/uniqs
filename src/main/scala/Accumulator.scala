import Accumulator.{Action, Click, Event, Impression}
import akka.actor.{Actor, ActorLogging, Props}
import com.redis._
import com.typesafe.config.Config

import scala.collection.mutable

class Accumulator(conf: Config, redis: RedisClient) extends Actor with ActorLogging {
  private val millisInHour = 60 * 60 * 1000

  private class HourlyData(chunksNumber: Int, hourStamp: Long) {

    private class Chunk(id: Int) {
      private var clicksCounter = 0L
      private var impressionCounter = 0L
      private var uids = List.empty[String]

      def add(uid: String, act: Action, hs: Long) = {
        uids = uid +: uids

        act match {
          case Click => clicksCounter += 1
          case Impression => impressionCounter += 1
        }

        if (clicksCounter + impressionCounter >= conf.getInt("maxChunkSize"))
          flush()
      }

      private def flush() = {
        redis.sadd(s"chunk:$hourStamp:$id", uids.head, uids.tail:_*)
        redis.incrby(s"clicks:$hourStamp", clicksCounter)
        redis.incrby(s"impressions:$hourStamp", impressionCounter)
        uids = Nil
        impressionCounter = 0
        clicksCounter = 0
        log.debug(s"Flushed: $hourStamp, $id")
      }
    }

    private val chunks = Array.tabulate(chunksNumber)(new Chunk(_))

    def add(uid: String, act: Action) = {
      val id = uid.hashCode.abs % chunksNumber
      chunks(id).add(uid, act, hourStamp)
    }
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
  }
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