package model

import akka.actor.{Actor, ActorLogging, Props}
import rule.RuleFactory

object ImdbHandler {
  def props(ruleFactory: RuleFactory): Props = Props(new ImdbHandler(ruleFactory))
  case class GetTogether(names: Set[String])
  case class GetMovie(title: String)
  case class GetName(name: String)
  case class GetTop(genre: String, qnt: Int, offset: Int)
}

class ImdbHandler(ruleFactory: RuleFactory) extends Actor with ActorLogging {
  import ImdbHandler._
  implicit val ec = context.dispatcher

  override def receive: Receive = {
    case GetMovie(title) =>
      val _sender = sender()
      ruleFactory.movieSearch.byTitle(title).foreach{ movies =>
        _sender ! movies
      }
    case GetName(name) =>
      val _sender = sender()
      ruleFactory.nameSearch.byName(name).foreach{ names =>
        _sender ! names
      }
    case GetTop(genre, qnt, off) =>
      val _sender = sender()
      ruleFactory.movieSearch.byGenre(genre, qnt, off).foreach( tops =>
        _sender ! tops
      )
    case GetTogether(names) =>
      val _sender = sender()
      ruleFactory.movieSearch.together(names).foreach( together =>
        _sender ! together
      )
  }
}
