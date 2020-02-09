import akka.actor.{ActorRef, ActorSystem}
import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestActorRef
import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import model.ImdbHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks
import response._
import rule.RuleFactory

import scala.concurrent.duration._

class ServiceSpec extends FunSuite
  with Matchers
  with ScalatestRouteTest
  with Service
  with ScalaFutures
  with MockFactory
  with TableDrivenPropertyChecks {
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5 seconds)
  override def testConfigSource = "akka.loglevel = DEBUG"
 // override val logger = NoLogging
  private def getPath(file: String) = getClass.getResource(file).getPath

  override def config = ConfigFactory.parseString(
    s"""
       |paths {
       |  nameBasics = "${getPath("name.basics.tsv")}"
       |  titleBasics = "${getPath("title.basics.tsv")}"
       |  titlePrincipals = "${getPath("title.principals.tsv")}"
       |  titleRatings = "${getPath("title.ratings.tsv")}"
       |}
    """.stripMargin)

  override val accumulator: ActorRef = ???
  override val redis: RedisClient = ???
}
