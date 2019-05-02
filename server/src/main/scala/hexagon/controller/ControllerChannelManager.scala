package hexagon.controller

import java.util.concurrent.{BlockingQueue, LinkedBlockingQueue}

import hexagon.api._
import hexagon.cluster.Broker
import hexagon.config.HexagonConfig
import hexagon.network.{BlockingChannel, Receive}
import hexagon.tools.{Logging, ShutdownableThread}

import scala.collection.mutable.HashMap

class ControllerChannelManager(controllerContext: ControllerContext, config: HexagonConfig) extends Logging {
  private val controllerBrokers = new HashMap[Int, ControllerBrokerWrapper]
  private val brokerLock = new Object

  controllerContext.liveBrokers.foreach(addNewBroker)


  def startup(): Unit = {
    brokerLock synchronized {
      controllerBrokers.foreach(broker => startRequestSendThread(broker._1))
    }
  }

  def shutdown(): Unit = {
    brokerLock synchronized {
      controllerBrokers.foreach(broker => removeExistingBroker(broker._1))
    }
  }

  def sendRequest(brokerId: Int, request: RequestOrResponse, callback: RequestOrResponse => Unit = null): Unit = {
    brokerLock synchronized {
      val stateInfoOpt = controllerBrokers.get(brokerId)
      stateInfoOpt match {
        case Some(stateInfo) =>
          stateInfo.messageQueue.put((request, callback))
        case None =>
          warn(s"Not sending request ${request.toString} to broker $brokerId, since it is offline.")
      }
    }
  }


  def removeBroker(brokerId: Int) {
    brokerLock synchronized {
      removeExistingBroker(brokerId)
    }
  }

  private def addNewBroker(broker: Broker) {
    val messageQueue = new LinkedBlockingQueue[(RequestOrResponse, RequestOrResponse => Unit)](config.controllerMessageQueueSize)
    debug(s"Controller ${config.brokerId} trying to connect to broker ${broker.id}")
    val channel = new BlockingChannel(broker.host, broker.port,
      BlockingChannel.UseDefaultBufferSize,
      BlockingChannel.UseDefaultBufferSize,
      config.controllerSocketTimeoutMs)
    val requestThread = new RequestSendThread(config.brokerId, controllerContext, broker, channel, messageQueue)
    requestThread.setDaemon(false)
    controllerBrokers.put(broker.id, ControllerBrokerWrapper(channel, broker, messageQueue, requestThread))
  }

  private def removeExistingBroker(brokerId: Int) {
    try {
      controllerBrokers(brokerId).channel.disconnect()
      controllerBrokers(brokerId).messageQueue.clear()
      controllerBrokers(brokerId).requestSendThread.shutdown()
      controllerBrokers.remove(brokerId)
    } catch {
      case e: Throwable => error("Error while removing broker by the controller", e)
    }
  }

  private def startRequestSendThread(brokerId: Int) {
    val requestThread = controllerBrokers(brokerId).requestSendThread
    if (requestThread.getState == Thread.State.NEW)
      requestThread.start()
  }
}


class RequestSendThread(val controllerId: Int,
                        val controllerCtx: ControllerContext,
                        val toBroker: Broker,
                        val channel: BlockingChannel,
                        val queue: BlockingQueue[(RequestOrResponse, RequestOrResponse => Unit)])
  extends ShutdownableThread(s"Controller-$controllerId-to-broker-${toBroker.id}-send-thread ") {

  private val logger = HexagonController.stateChangeLogger
  private val lock = new Object

  connectToBroker(toBroker, channel)

  override def doWork(): Unit = {
    val queueItem = queue.take()
    val request = queueItem._1
    val callback = queueItem._2
    var receive: Receive = null
    try {
      lock synchronized {
        var isSendSuccessful = false
        while (isRunning.get() && !isSendSuccessful) {
          // if a broker goes down for a long time, then at some point the controller's zookeeper listener will trigger a
          // removeBroker which will invoke shutdown() on this thread. At that point, we will stop retrying.
          try {
            channel.send(request)
            receive = channel.receive()
            isSendSuccessful = true
          } catch {
            case e: Throwable => // if the send was not successful, reconnect to broker and resend the message
              warn(s"Controller $controllerId epoch ${controllerCtx.epoch} fails to send request ${request.toString} to broker ${toBroker.toString}. " +
                "Reconnecting to broker.", e)
              channel.disconnect()
              connectToBroker(toBroker, channel)
              isSendSuccessful = false
              // backoff before retrying the connection and send
              swallow(Thread.sleep(300))
          }
        }
        var response: RequestOrResponse = null
        request.id match {
          case RequestKeys.LeaderAndIsr =>
            response = LeaderAndIsrResponse.readFrom(receive.buffer)
          case RequestKeys.StopReplica =>
            response = StopReplicaResponse.readFrom(receive.buffer)
          case RequestKeys.UpdateMetadata =>
            response = UpdateMetadataResponse.readFrom(receive.buffer)
        }
        logger.trace(s"Controller $controllerId epoch ${controllerCtx.epoch} received response ${response.toString} for a request sent to broker ${toBroker.toString}.")

        if (callback != null) {
          callback(response)
        }
      }
    } catch {
      case e: Throwable =>
        error(s"Controller $controllerId fails to send a request to broker ${toBroker.toString}.", e)
        // If there is any socket error (eg, socket timeout), the channel is no longer usable and needs to be recreated.
        channel.disconnect()
    }
  }


  private def connectToBroker(broker: Broker, channel: BlockingChannel) {
    try {
      channel.connect()
      info(s"Controller $controllerId connected to ${broker.toString} for sending state change requests")
    } catch {
      case e: Throwable => {
        channel.disconnect()
        error(s"Controller $controllerId's connection to broker ${broker.toString} was unsuccessful", e)
      }
    }
  }
}


case class ControllerBrokerWrapper(channel: BlockingChannel,
                                   broker: Broker,
                                   messageQueue: BlockingQueue[(RequestOrResponse, RequestOrResponse => Unit)],
                                   requestSendThread: RequestSendThread)

case class StopReplicaRequestInfo(replica: Int, deleteTopic: Boolean, callback: RequestOrResponse => Unit = null)