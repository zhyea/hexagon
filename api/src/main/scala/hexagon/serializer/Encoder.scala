package hexagon.serializer

import java.nio.charset.StandardCharsets

trait Encoder[T] {

  def toBytes(t: T): Array[Byte]

}


class DefaultEncoder extends Encoder[Array[Byte]] {

  override def toBytes(arr: Array[Byte]): Array[Byte] = arr

}


class StringEncoder extends Encoder[String] {

  override def toBytes(s: String): Array[Byte] = {
    if (null == s)
      null
    else
      s.getBytes(StandardCharsets.UTF_8)
  }

}