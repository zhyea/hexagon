package hexagon.controller

sealed trait BrokerStates {
  def state: Byte
}

case object NotRunning extends BrokerStates { val state: Byte = 0 }
case object Starting extends BrokerStates { val state: Byte = 1 }
case object RecoveringFromUncleanShutdown extends BrokerStates { val state: Byte = 2 }
case object RunningAsBroker extends BrokerStates { val state: Byte = 3 }
case object RunningAsController extends BrokerStates { val state: Byte = 4 }
case object PendingControlledShutdown extends BrokerStates { val state: Byte = 6 }
case object BrokerShuttingDown extends BrokerStates { val state: Byte = 7 }


case class BrokerState() {

  @volatile var currentState: Byte = NotRunning.state

  def newState(newState: BrokerStates) {
    this.newState(newState.state)
  }

  // Allowing undefined custom state
  def newState(newState: Byte) {
    currentState = newState
  }
}

