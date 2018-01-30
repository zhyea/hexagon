import hexagon.tools.Logging

object Hexagon extends Logging {


  def main(args: Array[String]): Unit = {
    try {

    } catch {
      case e: Exception => error("Error during hexagon startup ", e)
    }


  }


}