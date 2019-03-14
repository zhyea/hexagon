package hexagon.bloom

import java.util

import hexagon.utils.Args._
import java.lang.Long._

private[bloom] object BitArray {

  def divide(p: Long, q: Long): Int = {
    val div = p / q
    val rem = p - q * div

    var longV = div
    if (0 != rem) {
      val signum = 1 | ((p ^ q) >> (SIZE - 1))
      longV = if (signum > 0) div + signum else div
    }

    val result = longV.toInt
    check(result == longV, s"Out of range: $longV")

    result
  }
}


private[bloom] class BitArray(val data: Array[Long]) {

  def this(bits: Long) {
    this(new Array[Long](BitArray.divide(bits, 64)))
  }

  check(data.length > 0, "data length is zero")
  var _bitCount: Long = data.fold(0)(bitCount(_) + bitCount(_))


  def set(index: Long): Boolean = {
    if (!get(index)) {
      data((index >>> 6).toInt) |= (1L << index)
      _bitCount = _bitCount + 1
      true
    } else {
      false
    }
  }


  def get(index: Long): Boolean = (data((index >>> 6).toInt) & (1L << index)) != 0


  def getBitCount: Long = _bitCount


  def bitSize(): Long = 1L * data.length * SIZE


  def copy(): BitArray = new BitArray(data.clone())


  def putAll(array: BitArray): Unit = {
    check(data.length == array.data.length,
      s"BitArrays must be of equal length (${data.length} != ${array.data.length})")
    _bitCount = 0
    for (i <- 0 to data.length) {
      data(i) |= array.data(i)
      _bitCount += bitCount(data(i))
    }
  }


  override def hashCode(): Int = util.Arrays.hashCode(data)


  override def equals(o: Any): Boolean = {
    o match {
      case bitArr: BitArray => util.Arrays.equals(data, bitArr.data)
      case _ => false
    }
  }
}
