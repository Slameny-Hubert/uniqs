import akka.actor.ActorSystem
import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.testkit.TestActorRef
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
  override val logger = NoLogging
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

  val ruleFactory = new RuleFactory(config)
  val imdbHandler: TestActorRef[ImdbHandler] = TestActorRef[ImdbHandler](new ImdbHandler(ruleFactory))

  val fredAstaire = NameResponse("nm0000001", "Fred Astaire", List("documentary", "short"), Some(1))
  val kevinBacon = NameResponse("nm0000002", "Kevin Bacon", List("documentary", "short"), Some(0))
  val brigitteBardot1 = NameResponse("nm0000003", "Brigitte Bardot", List(), Some(2))
  val brigitteBardot2 = NameResponse("nm0000004", "Brigitte Bardot", List(), None)
  val typecasted12 = NameResponse("nm0000005", "Typecasted12", List("genre1", "genre2"), None)
  val casted3 = NameResponse("nm0000006", "casted3", List(), None)
  val notCasted = NameResponse("nm0000007", "Not casted", List(), None)
  val castedInUndefinedMovie = NameResponse("nm0000008", "Undef Casted", List(), None)

  val nameCases = Table(
    ("name", "url", "response"),
    ("Bacon distance=1", "Fred%20Astaire", List(fredAstaire)),
    ("No such name", "Fred%20Astaire1", List.empty[NameResponse]),
    ("Same name. 1: Bacon=2. casted without genres, 2: No bacon distance. casted in 3 movie with genres, but not typecasted", "Brigitte%20Bardot", List(brigitteBardot1, brigitteBardot2)),
    ("Case doesn't matter", "fReD%20aStAiRe", List(fredAstaire)),
    ("Bacon distance of Kevin Bacon is 0. typecasted in 1 movie 2 genres", "Kevin%20Bacon", List(kevinBacon)),
    ("Typecasted in 2 genre/2 movies", "Typecasted12", List(typecasted12)),
    ("Casted in 3 movies, but only 1 with genre", "Typecasted12", List(typecasted12)),
    ("No movie for this actor", "Not%20Casted", List(notCasted)),
    ("Casted in undefined movie", "Undef%20Casted", List(castedInUndefinedMovie))
  )

  forAll(nameCases) { (name, url, response) =>
    test(name){
      Get(s"/api/name/$url") ~> routes ~> check {
        status shouldBe OK
        responseAs[List[NameResponse]] shouldBe response
      }
    }
  }

  val carmecita = MovieResponse(
    "tt0000001",
    "Carmencita",
    "aaa",
    false,
    Some(1894),
    None,
    Some(1),
    "documentary,short",
    List(fredAstaire, kevinBacon)
  )

  val battleshipPotemkin = MovieResponse(
    "tt0000011",
    "Movie without actors",
    "Броненосец \"Потемкин\"",
    false,
    Some(1892),
    None,
    Some(5),
    "",
    List()
  )

  val brainDead1 = MovieResponse("tt0000012", "Brain Dead", "", false, None, None, None, "", List())
  val brainDead2 = MovieResponse("tt0000013", "Brain Dead", "", false, None, None, None, "", List())
  val meetTheFeebles1 = MovieResponse("tt0000014", "t1", "Meet the Feebles", false, None, None, None, "", List())
  val meetTheFeebles2 = MovieResponse("tt0000015", "t2", "Meet the Feebles", false, None, None, None, "", List())
  val manBitesDog1 = MovieResponse("tt0000016", "Man Bites Dog", "ot1", false, None, None, None, "", List())
  val manBitesDog2 = MovieResponse("tt0000017", "t2", "Man Bites Dog", false, None, None, None, "", List())

  val pi = MovieResponse("tt0000018", "Pi", "", true, None, Some(1), None, "", List(
    NameResponse("nm0000010", "Mihail Gorokhovsky", List(), None)
  ))

  val leClown = MovieResponse(
    "tt0000002",
    "Le clown et ses chiens",
    "Le clown et ses chiens",
    false,
    Some(1892),
    None,
    Some(5),
    "",
    List(fredAstaire, brigitteBardot1)
  )

  val movieCases = Table(
    ("name", "url", "response"),
    ("Movie with actors and genres by prime title.", "Carmencita", List(carmecita)),
    ("Movie without actors and genres by original title.", "%D0%91%D1%80%D0%BE%D0%BD%D0%B5%D0%BD%D0%BE%D1%81%D0%B5%D1%86%20%22%D0%9F%D0%BE%D1%82%D0%B5%D0%BC%D0%BA%D0%B8%D0%BD%22", List(battleshipPotemkin)),
    ("Case doesn't matter: Latin", "cArMeNcItA", List(carmecita)),
    ("Case doesn't matter: non Latin", "%D0%B1%D0%A0%D0%BE%D0%9D%D0%B5%D0%9D%D0%BE%D0%A1%D0%B5%D0%A6%20%22%D0%BF%D0%9E%D1%82%D0%95%D0%BC%D0%9A%D0%B8%D0%9D%22", List(battleshipPotemkin)),
    ("No such movie", "Mihail%20Gorokhovsky%20saves%20the%20World", List()),
    ("2 movies by primary title", "Brain%20dead", List(brainDead1, brainDead2)),
    ("2 movies by original title", "Meet%20the%20Feebles", List(meetTheFeebles1, meetTheFeebles2)),
    ("2 movies by prime and original title", "Man%20Bites%20Dog", List(manBitesDog1, manBitesDog2)),
    ("Movie with undef actor", "Pi", List(pi))
  )

  forAll(movieCases) { (name, url, response) =>
    test(name){
      Get(s"/api/movie/$url") ~> routes ~> check {
        status shouldBe OK
        responseAs[List[MovieResponse]] shouldBe response
      }
    }
  }

  val together1 = NameResponse("nm0000011", "Together1", List(), None)
  val together2 = NameResponse("nm0000012", "Together2", List(), None)
  val together3 = NameResponse("nm0000013", "Together3", List(), None)

  val movieTogether3 = MovieResponse(
    "tt0000019",
    "Together3",
    "",
    true,
    None,
    None,
    None,
    "",
    List(together1, together2, together3)
  )

  val togetherCases = Table(
    ("name", "names", "response"),
    ("One name", Seq("Fred%20Astaire"), List(carmecita, leClown)),
    ("2 names with intersection", Seq("Fred%20Astaire", "Kevin%20Bacon"), List(carmecita)),
    ("2 names without intersection", Seq("Fred%20Astaire", "Typecasted12"), List()),
    ("3 names with intersection (1 and 2 were in the 2nd movie, but without 3)", Seq("Together1", "Together2", "Together3"), List(movieTogether3)),
    ("1 of the names is unknown", Seq("Fred%20Astaire", "NoSuchActor"), List()),
  )

  forAll(togetherCases) { (name, names, response) =>
    test(name){
      val parameters = names.map("name=" + _).mkString("&")
      Get(s"/api/together?$parameters") ~> routes ~> check {
        status shouldBe OK
        responseAs[List[MovieResponse]] shouldBe response
      }
    }
  }

  val m3 = MovieResponse(
    "tt0000003",
    "Movie with genre1 and genre2",
    "Le clown et ses chiens",
    false,
    Some(1892),
    None,
    Some(5),
    "genre1,genre2",
    List(brigitteBardot2)
  )

  val m6 = MovieResponse(
    "tt0000006",
    "Movie with genre1",
    "Le clown et ses chiens",
    false,
    Some(1892),
    None,
    Some(5),
    "genre1",
    List(typecasted12)
  )

  val topCases = Table(
    ("name", "genre", "qnt", "off", "response"),
    ("qnt > answer size, off=0", "Documentary", 2, 0, List(TopResponse(carmecita, "5.8"))),
    ("qnt = answer size, off=0", "genre1", 2, 0, List(TopResponse(m3, "6.6"), TopResponse(m6, "6.2"))),
    ("qnt < answer size, off=0", "genre1", 1, 0, List(TopResponse(m3, "6.6"))),
    ("qnt < answer size, off=1", "genre1", 1, 1, List(TopResponse(m6, "6.2"))),
    ("off=answer size", "genre1", 1, 2, List()),
    ("Genre case doesn't matter", "GeNrE1", 2, 0, List(TopResponse(m3, "6.6"), TopResponse(m6, "6.2"))),
  )

  forAll(topCases) { (name, genre, qnt, off, response) =>
    test(name){
      Get(s"/api/top/$genre?qnt=$qnt&off=$off") ~> routes ~> check {
        status shouldBe OK
        responseAs[List[TopResponse]] shouldBe response
      }
    }
  }

  test("Wrong genre"){
    Get(s"/api/top/noSuchGenre?qnt=1&off=0") ~> Route.seal(routes) ~> check {
      status shouldBe NotFound
    }
  }

  test("Wrong qnt"){
    Get(s"/api/top/genre1?qnt=0&off=0") ~> Route.seal(routes) ~> check {
      status shouldBe BadRequest
    }
  }

  test("Wrong off"){
    Get(s"/api/top/genre1?qnt=1&off=-1") ~> Route.seal(routes) ~> check {
      status shouldBe BadRequest
    }
  }
}
