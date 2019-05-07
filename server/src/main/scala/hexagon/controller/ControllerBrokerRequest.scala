package hexagon.controller

import hexagon.api.{RequestOrResponse, TopicStateInfo}
import hexagon.config.HexagonConfig
import hexagon.tools.Logging

import scala.collection.mutable

class ControllerBrokerRequest(controllerContext: ControllerContext,
                              config: HexagonConfig) extends Logging {

  /**
    * leader
    */
  val leaderAndIsrRequestMap = new mutable.HashMap[Int, mutable.HashMap[String, TopicStateInfo]]


  def addLeaderAndIsrRequestForBroker(brokerId: Int,
                                      topic: String,
                                      leaderIsrAndControllerEpoch: LeaderIsrAndControllerEpoch,
                                      replicas: Seq[Int],
                                      callback: RequestOrResponse => Unit = null): Unit = {

  }


}
