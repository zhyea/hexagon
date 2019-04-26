package hexagon.controller

import java.util.concurrent.locks.ReentrantLock

import hexagon.zookeeper.ZkClient

class ControllerContext(val zkClient: ZkClient,
                        val zkSessionTimeout: Int) {

  val controllerLock: ReentrantLock = new ReentrantLock()


}
