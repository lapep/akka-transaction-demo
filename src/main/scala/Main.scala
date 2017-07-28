import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MalformedRequestContentRejection, RejectionHandler}
import akka.stream.ActorMaterializer
import routes.TransactionRoutes
import services.{Account, TransferCoordinator}

import scala.concurrent.ExecutionContext

object Main extends App {
  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case MalformedRequestContentRejection(msg, err) =>
        complete(HttpResponse(StatusCodes.BadRequest, entity = msg))
      }
      .handleNotFound {
        complete((StatusCodes.NotFound, "Requested entity not found"))
      }
      .result()

  implicit val actorSystem = ActorSystem()
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val log: LoggingAdapter = Logging(actorSystem, getClass)
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  //hashMap just for illustration purposes to store the user accounts
  val userAccounts = scala.collection.mutable.HashMap[Long, BigDecimal](1L -> 100, 2L -> 100)

  val accountActor = actorSystem.actorOf(Account.props(userAccounts), "accountActor")

  val transferCoordinator = actorSystem.actorOf(TransferCoordinator.props(accountActor), "transferCoordinatorActor")

  val refreshRoutes = new TransactionRoutes(transferCoordinator)

  val allRoutes = refreshRoutes.routes

  Http().bindAndHandle(allRoutes, "0.0.0.0", 3000)
}
