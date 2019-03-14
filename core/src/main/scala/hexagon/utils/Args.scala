package hexagon.utils

object Args {


  def check(expression: Boolean): Unit =
    if (!expression)
      throw new IllegalArgumentException()


  def check(expression: Boolean, errorMessages: String): Unit =
    if (!expression)
      throw new IllegalArgumentException(errorMessages)


  def checkState(expression: Boolean): Unit =
    if (!expression)
      throw new IllegalStateException()


  def checkState(expression: Boolean, errorMessage: String): Unit =
    if (!expression)
      throw new IllegalStateException(errorMessage)


  def checkNotNull[T](reference: T): Unit =
    if (null == reference)
      throw new NullPointerException()


  def checkNotNull[T](reference: T, errorMessage: String): Unit =
    if (null == reference)
      throw new NullPointerException(errorMessage)
}
