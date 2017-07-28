package services

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.util.Timeout
import services.Account.Deposit

import scala.concurrent.duration._

class TransferCoordinator(accountActor: ActorRef) extends Actor {

  implicit val timeout = Timeout(1.seconds)

  import TransferCoordinator._

  def receive = LoggingReceive {
    case Transaction(from, to, amount) =>
      accountActor ! Account.Withdraw(from, amount)
      context.become(receiveAfterWithdraw(from, to, amount, sender))
  }

  def receiveAfterWithdraw(from: Long, to: Long, amount: BigDecimal, parentActor: ActorRef): Receive = LoggingReceive {
    case Account.Completed =>
      accountActor ! Account.Deposit(to, amount)
      context.become(receiveAfterDeposit(from, to, amount, parentActor))

    case Account.Failed(msg) =>
      parentActor ! Failed(msg, to, amount)
      context.become(receive)
  }

  def receiveAfterDeposit(from: Long, to: Long, amount: BigDecimal, parentActor: ActorRef, isRollBack: Boolean = false): Receive = LoggingReceive {
    case Account.Completed if isRollBack =>
      parentActor ! Failed("Transaction Error, transaction rolled back", to, amount)
      context.become(receive)

    case Account.Completed =>
      parentActor ! Done
      context.become(receive)

    case Account.Failed(msg) if isRollBack =>
      parentActor ! Failed(msg + ": Transaction rollback failed", to, amount)
      context.become(receive)

    case Account.Failed(msg) =>
      accountActor ! Deposit(from, amount)
      context.become(receiveAfterDeposit(from, to, amount, parentActor, isRollBack = true))
  }
}

object TransferCoordinator {

  case class Transaction(sender: Long, receiver: Long, amount: BigDecimal)

  case object Done

  case class Failed(reason: String, account: Long, amount: BigDecimal)

  def props(accountActor: ActorRef) = Props(classOf[TransferCoordinator], accountActor)
}