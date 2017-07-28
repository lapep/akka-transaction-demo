import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import services.Account
import services.Account.{Completed, Deposit, Failed, Withdraw}

class AccountTest extends TestKit(ActorSystem("Test")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  private trait TestFixture {
    val hashMap = scala.collection.mutable.HashMap[Long, BigDecimal](1L -> 100, 2L -> 100)
    val probe = TestProbe()
    val accountActor = system.actorOf(Props(classOf[Account], hashMap))
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Money Account actor" must {
    "send back status Completed when successfully processes Deposit" in new TestFixture {
      probe.send(accountActor, Deposit(1L, 50.00))
      probe.expectMsg(Completed)
    }

    "send back status Failure when trying to deposit a negative amount" in new TestFixture {
      probe.send(accountActor, Deposit(1L, -50.00))
      probe.expectMsg(Failed("Negative amount is not allowed: -50.0"))
    }

    "send back status Completed when successfully processes Withdrawal" in new TestFixture {
      probe.send(accountActor, Withdraw(1L, 50.00))
      probe.expectMsg(Completed)
    }

    "send back status Failure when there are not enough funds during Withdrawal" in new TestFixture {
      probe.send(accountActor, Withdraw(1L, 100.01))
      probe.expectMsg(Failed("Not enough funds in account: 1, current funds: 100"))
    }

    "send back status Failure when trying to withdraw a negative amount" in new TestFixture {
      probe.send(accountActor, Withdraw(1L, -1))
      probe.expectMsg(Failed("Negative amount is not allowed: -1"))
    }

    "send back status Failure when sending unsupported message" in new TestFixture {
      case object Unsupported

      probe.send(accountActor, Unsupported)
      probe.expectMsg(Failed("No such operation exist"))
    }
  }
}