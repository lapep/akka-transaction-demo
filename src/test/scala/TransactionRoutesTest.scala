import akka.actor.Actor
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestKitBase}
import akka.util.ByteString
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import routes.TransactionRoutes
import services.TransferCoordinator.{Done, Failed}

import scala.concurrent.Await
import scala.concurrent.duration._

class TransactionRoutesTest extends WordSpec with MockitoSugar with Matchers with ScalatestRouteTest with TestKitBase with ImplicitSender {
  implicit val ec = system.dispatcher
  implicit def rejectionHandler = Main.rejectionHandler

  private trait TestFixture {
    val actor = TestActorRef(new Actor {override def receive: Receive = {case _ => sender ! Done}})
    val refreshRoutes = new TransactionRoutes(actor)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val successfulJson = ByteString(
    s"""
       |{
       |    "sender":"1",
       |    "receiver":"2",
       |    "amount":"20.00"
       |}
        """.stripMargin)

  "Refresh API" must {

    "return 200 given a valid input JSON" in new TestFixture {
      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/transaction",
        entity = HttpEntity(MediaTypes.`application/json`, successfulJson))

      postRequest ~> refreshRoutes.routes ~> check {
        status.isSuccess() shouldEqual true
        response.status shouldBe StatusCodes.OK
      }
    }

    "return 400 Bad Request given invalid input JSON" in new TestFixture {
      val jsonRequest = ByteString(
        s"""
           |{
           |    "xxx":"abc"
           |}
        """.stripMargin)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/transaction",
        entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~> Route.seal(refreshRoutes.routes) ~> check {
        status.isSuccess() shouldEqual false
        response.status shouldBe StatusCodes.BadRequest
      }
    }

    "return 404 Not Found given invalid URI" in new TestFixture {
      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/xxx",
        entity = HttpEntity(MediaTypes.`application/json`, successfulJson))

      postRequest ~> Route.seal(refreshRoutes.routes) ~> check {
        status.isSuccess() shouldEqual false
        response.status shouldBe StatusCodes.NotFound
      }
    }

    "return 200 with error response given system failed to update balance" in  {
      val actor = TestActorRef(new Actor {override def receive: Receive = {case _ => sender ! Failed("Error", 1L, 20.00)}})
      val refreshRoutes = new TransactionRoutes(actor)

      val postRequest = HttpRequest(
        HttpMethods.POST,
        uri = "/transaction",
        entity = HttpEntity(MediaTypes.`application/json`, successfulJson))

      postRequest ~> Route.seal(refreshRoutes.routes) ~> check {
        status.isSuccess() shouldEqual true
        response.status shouldBe StatusCodes.OK
        val body = Await.result(response.entity.toStrict(2.seconds), 2.seconds)
        body.data.utf8String shouldBe """{"sender":1,"receiver":2,"amount":20.0,"message":"Failed","reason":"Error"}"""
      }
    }
  }

}
