package hexagon

import hexagon.server.{HexagonServer, HexagonServerStartable}
import hexagon.tools.{Logging, PropKit}
import hexagon.config.HexagonConfig

object Hexagon extends Logging {


  def main(args: Array[String]): Unit = {

    if (args.length != 1) {
      println(s"Usage: java [options] ${classOf[HexagonServer].getSimpleName} server.properties")
      System.exit(1)
    }

    try {
      val props = PropKit.load(args(0))
      val config = new HexagonConfig(props)

      val serverStartable = new HexagonServerStartable(config)
      Runtime.getRuntime
        .addShutdownHook(new Thread(() => serverStartable.shutdown()))

      serverStartable.startup()
      serverStartable.awaitShutdown()

    } catch {
      case e: Exception => error("Error during hexagon startup ", e)
    }

    System.exit(1)
  }


}