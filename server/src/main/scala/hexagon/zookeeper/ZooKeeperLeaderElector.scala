package hexagon.zookeeper

import hexagon.controller.ControllerContext

class ZooKeeperLeaderElector(controllerContext: ControllerContext,
                             electionPath:String,
                             onBecomingLeader: ()=>Unit,
                             onResigningAsLeader: ()=>Unit,
                             brokerId:Int) {

}
