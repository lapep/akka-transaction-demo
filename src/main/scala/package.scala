package object api {
  sealed trait TransactionError
  final case class TransactionFailure(message: String) extends TransactionError

  case class apiError(reason: String)

  case class TransactionRequest(sender: Long, receiver: Long, amount: Double)
  case class TransactionResponse(sender: Long, receiver: Long, amount: Double, message: String, info: String = "Success")
}