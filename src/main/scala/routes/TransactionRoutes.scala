package routes

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout
import api.{TransactionRequest, TransactionResponse, apiError}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import services.TransferCoordinator.{Done, Failed, Transaction}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TransactionRoutes(transferCoordinator: ActorRef)(implicit actorSystem: ActorSystem) {
  implicit val timeout = Timeout(1.seconds)

  val routes = {
    path("transaction") {
        post {
          entity(as[TransactionRequest]) { rq =>
              onComplete(transferCoordinator ? Transaction(rq.sender, rq.receiver, rq.amount)) {
                case Success(res) => res match {
                  case Done =>
                    complete(StatusCodes.OK, TransactionResponse(rq.sender, rq.receiver, rq.amount, "Completed"))
                  case Failed(reason, account, amount) =>
                    complete(StatusCodes.OK, TransactionResponse(rq.sender, rq.receiver, rq.amount, "Failed", reason))
                }
                case Failure(err) => complete(StatusCodes.InternalServerError, apiError(err.getLocalizedMessage))
              }

          }
        }
    }
  }
}