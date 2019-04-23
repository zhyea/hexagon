package hexagon.controller

import java.util.concurrent.locks.ReentrantLock

import hexagon.network.ZkClient

class ControllerContext(val zkClient: ZkClient) {

  val controllerLock:ReentrantLock = new ReentrantLock()


}
