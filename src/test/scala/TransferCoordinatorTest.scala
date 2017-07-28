import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import services.Account.{Deposit, Withdraw, Failed => accFailed}
import services.TransferCoordinator.{Done, Failed, Transaction}
import services.{Account, TransferCoordinator}

import scala.concurrent.duration._

class TransferCoordinatorTest extends TestKit(ActorSystem("Test")) with WordSpecLike with Matchers with BeforeAndAfterAll {

  implicit val timeout: Timeout = Timeout(5.seconds)

  private trait TestFixture {
    val probe = TestProbe()
    val transferCoordinator = system.actorOf(TransferCoordinator.props(probe.ref))
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Transfer Coordinator actor" must {
    "return status Done if transaction is processed successfully" in new TestFixture {
      probe.send(transferCoordinator, Transaction(1L, 2L, 50))
      probe.expectMsg(Withdraw(1, 50))
      probe.reply(Account.Completed)
      probe.expectMsg(Deposit(2, 50))
      probe.reply(Account.Completed)
      probe.expectMsg(Done)
    }

    "return status Failed if withdrawal fails" in new TestFixture {
      probe.send(transferCoordinator, Transaction(1L, 2L, 50))
      probe.expectMsg(Withdraw(1, 50))
      probe.reply(accFailed("error"))
      probe.expectMsg(Failed("error", 2, 50))
    }

    "return status Failed and do a roll back if deposit fails" in new TestFixture {
      probe.send(transferCoordinator, Transaction(1L, 2L, 50))
      probe.expectMsg(Withdraw(1, 50))
      probe.reply(Account.Completed)
      probe.expectMsg(Deposit(2, 50))
      probe.reply(Account.Failed("error"))
      probe.expectMsg(Deposit(1, 50))
      probe.reply(Account.Completed)
      probe.expectMsg(Failed("Transaction Error, transaction rolled back", 2, 50))
    }

    "return status Failed and if roll back fails" in new TestFixture {
      probe.send(transferCoordinator, Transaction(1L, 2L, 50))
      probe.expectMsg(Withdraw(1, 50))
      probe.reply(Account.Completed)
      probe.expectMsg(Deposit(2, 50))
      probe.reply(Account.Failed("error"))
      probe.expectMsg(Deposit(1, 50))
      probe.reply(Account.Failed("error"))
      probe.expectMsg(Failed("error: Transaction rollback failed", 2, 50))
    }
  }
}