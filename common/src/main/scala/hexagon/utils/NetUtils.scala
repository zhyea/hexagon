package hexagon.utils

import java.nio.channels.{SelectionKey, SocketChannel}
import java.util.regex.Pattern

object NetUtils {


  private val HOST_PORT_PATTERN = Pattern.compile("\\[?(.+?)\\]?:(\\d+)")


  def channelOf(key: SelectionKey): SocketChannel = {
    key.channel().asInstanceOf[SocketChannel]
  }
  

  def extractHost(address: String): String = {
    val matcher = HOST_PORT_PATTERN.matcher(address)
    if (matcher.matches) matcher.group(1)
    else null
  }


  def extractPort(address: String): Integer = {
    val matcher = HOST_PORT_PATTERN.matcher(address)
    if (matcher.matches) matcher.group(2).toInt
    else null
  }


  def formatAddress(host: String, port: Integer): String = {
    if (host.contains(":")) "[" + host + "]:" + port // IPv6
    else host + ":" + port
  }
}
