package services

import akka.actor.{Actor, Props}
import akka.event.LoggingReceive
import api._

class Account(userAccounts: scala.collection.mutable.HashMap[Long, BigDecimal]) extends Actor {

  import Account._

  def receive = LoggingReceive {

    case Deposit(user, amount) =>
      deposit(user, amount) match {
        case Right(_) => sender ! Completed
        case Left(err) => sender ! Failed(err.message)
      }

    case Withdraw(user, amount) =>
      withdraw(user, amount) match {
        case Right(_) => sender ! Completed
        case Left(err) => sender ! Failed(err.message)
      }

    case _ => sender ! Failed("No such operation exist")
  }

  private def deposit(user: Long, amount: BigDecimal) = {
    if (userAccounts.get(user).isEmpty) Left(TransactionFailure(s"Account $user does not exist"))
    else {
      if(amount < 0)
        Left(TransactionFailure(s"Negative amount is not allowed: $amount"))
      else
        Right(userAccounts.put(user, userAccounts(user) + amount))
    }
  }

  private def withdraw(user: Long, amount: BigDecimal) = {
    if (userAccounts.get(user).isEmpty) Left(TransactionFailure(s"Account $user does not exist"))
    else {
      if(amount < 0)
        Left(TransactionFailure(s"Negative amount is not allowed: $amount"))
      else if (userAccounts(user) - amount < 0)
        Left(TransactionFailure(s"Not enough funds in account: $user, current funds: ${userAccounts(user)}"))
      else
        Right(userAccounts.put(user, userAccounts(user) - amount))
    }
  }
}

object Account {

  case object Completed

  case class Withdraw(user: Long, amount: BigDecimal)

  case class Deposit(user: Long, amount: BigDecimal)

  case class Failed(reason: String)

  def props(userAccounts: scala.collection.mutable.HashMap[Long, BigDecimal]) =
    Props(classOf[Account], userAccounts)
}