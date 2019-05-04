package hexagon.exceptions


class LeaderElectionNotNeededException(message: String, t: Throwable) extends RuntimeException(message, t) {

  def this(message: String) = this(message, null)

  def this(t: Throwable) = this("", t)

}
