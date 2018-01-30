package hexagon.server

import hexagon.config.HexagonConfig
import hexagon.tools.Logging

class HexagonServerStartable(config: HexagonConfig) extends Logging {

  private val server: HexagonServer = new HexagonServer(config)

  def startup(): Unit ={
    try{
      server.startup()
    }catch{
      case e:Exception =>{
        error("Error during hexagonStartable startup.", e)
        server.shutdown()
        System.exit(1)
      }
    }
  }


  def awaitShutdown(): Unit ={
    server.awaitShutdown()
  }


  def shutdown(): Unit ={
    try{
      server.shutdown()
    }catch{
      case e:Exception =>{
        error("Error during hexagonStartable shutdown.", e)
        System.exit(1)
      }
    }
  }


}
