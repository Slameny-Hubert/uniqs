package com.mgorokhovsky.it

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{DateTime, HttpMethods, HttpRequest, HttpResponse}
import akka.util.ByteString
import org.scalatest._

import scala.util.{Success, Try}

class ServiceSpec extends AsyncFlatSpec {

  implicit val system = ActorSystem()

  val now = DateTime.now.clicks
  val hourAgo = now - 60 * 60 * 1000

  // set of POST-requests
  Seq(
    (now,     "Misha", "click"),
    (now,     "Misha", "click"),
    (now,     "Freddy", "click"),
    (now,     "Misha", "impression"),
    (hourAgo, "Misha", "impression"),
    (hourAgo, "Misha", "impression"),
    (hourAgo, "Misha", "impression"),
  )
    .foreach { case (ts, uid, act) =>
      Http().singleRequest(HttpRequest(
        method = HttpMethods.POST,
        uri = s"http://0.0.0.0:8080/analytics?timestamp=$ts&user=$uid&$act"
      )).onComplete { case Success(_) => }
    }

  Thread.sleep(2000)

  // set of GET-requests and expected responses
  val expected = Map(
    now     -> (2, 3, 1),
    hourAgo -> (1, 0, 3)
  )

  it should "get correct responses" in {
    Http()
      .singleRequest( HttpRequest(
        method = HttpMethods.GET,
        uri = s"http://0.0.0.0:8080/analytics?timestamp=$now"
      ))
      .flatMap(_.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { _.utf8String})
      .map { txt => assert(txt === getResp(expected(now)))}
  }

  def getResp(ans: (Int, Int, Int)) = s"unique_users,${ans._1}\nclicks,${ans._2}\nimpressions,${ans._3}"
}
