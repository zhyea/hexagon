package hexagon.controller


/**
  * Controller为topic进行leader选举时会抛出此异常，因为所有的leader候选都已下线
  */
class NoReplicaOnlineException(message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def this(message: String) = this(message, null)

  def this() = this(null, null)

}
