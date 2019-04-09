package hexagon

import hexagon.server.{HexagonServer, HexagonServerBootable}
import hexagon.utils.PropKit
import hexagon.config.HexagonConfig
import hexagon.tools.Logging

object Hexagon extends Logging {


  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      println(s"Usage: java [options] ${classOf[HexagonServer].getSimpleName} server.properties")
      System.exit(1)
    }

    try {
      val props = PropKit.load(args(0))
      val config = new HexagonConfig(props)

      val serverBootable = new HexagonServerBootable(config)

      Runtime.getRuntime
        .addShutdownHook(new Thread(() => serverBootable.shutdown()))

      serverBootable.startup()
      serverBootable.awaitShutdown()

    } catch {
      case e: Exception => error("Error during hexagon startup ", e)
    }

    System.exit(1)
  }


}