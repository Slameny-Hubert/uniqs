import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import model.ImdbHandler
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContextExecutor
import rule.RuleFactory

case class UserPwd(pwd:String)

case class UpsertRequest(username:String, password:String )


trait Protocols extends DefaultJsonProtocol {
  implicit val nameFormat = jsonFormat4(response.NameResponse.apply)
  implicit val movieFormat = jsonFormat9(response.MovieResponse.apply)
  implicit val topFormat = jsonFormat2(response.TopResponse.apply)
}

trait Service extends Protocols {
  import scala.concurrent.duration._

  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  implicit def config: Config
  val logger: LoggingAdapter
  def imdbHandler: ActorRef
  implicit def requestTimeout = Timeout(5 seconds)

  val routes: Route = {
  pathPrefix("api") {
    pathPrefix("movie") {
      path(Segment) { title => {
        complete((imdbHandler ? ImdbHandler.GetMovie(title)).mapTo[Seq[response.MovieResponse]])
      }}
    } ~
      pathPrefix("name") {
        path(Segment) { name => {
          complete((imdbHandler ? ImdbHandler.GetName(name)).mapTo[Seq[response.NameResponse]])
        }}
      } ~
      pathPrefix("top") {
        pathPrefix(Segment) { genre => {
          parameters('qnt.as[Int], 'off.as[Int]) { (qnt, off) =>
            validate(qnt > 0, "Wrong quantity. qnt must be positive.") {
              validate(off >= 0, "Wrong offset. off must be non negative") {
                rejectEmptyResponse {
                  complete((imdbHandler ? ImdbHandler.GetTop(genre, qnt, off)).mapTo[Option[Seq[response.TopResponse]]])
                }
              }
            }
          }
        }}
      } ~
      pathPrefix("together") {
        parameters('name, 'name.*) { (name, names) =>
          complete((imdbHandler ? ImdbHandler.GetTogether(names.toSet + name)).mapTo[Seq[response.MovieResponse]])
        }
      }
    }
  }
}

object HttpService extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()
  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)
  val ruleFactory = new RuleFactory(config)
  val imdbHandler = system.actorOf(ImdbHandler.props(ruleFactory))

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
