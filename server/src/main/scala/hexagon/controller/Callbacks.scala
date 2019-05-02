package hexagon.controller

import hexagon.api.RequestOrResponse


object Callbacks {

  class CallbackBuilder {

    var leaderAndIsrResponseCbk: RequestOrResponse => Unit = null
    var updateMetadataResponseCbk: RequestOrResponse => Unit = null
    var stopReplicaResponseCbk: (RequestOrResponse, Int) => Unit = null

    def leaderAndIsrCallback(cbk: RequestOrResponse => Unit): CallbackBuilder = {
      leaderAndIsrResponseCbk = cbk
      this
    }

    def updateMetadataCallback(cbk: RequestOrResponse => Unit): CallbackBuilder = {
      updateMetadataResponseCbk = cbk
      this
    }

    def stopReplicaCallback(cbk: (RequestOrResponse, Int) => Unit): CallbackBuilder = {
      stopReplicaResponseCbk = cbk
      this
    }

    def build: Callbacks = {
      new Callbacks(leaderAndIsrResponseCbk, updateMetadataResponseCbk, stopReplicaResponseCbk)
    }
  }

}


class Callbacks private(var leaderAndIsrResponseCallback: RequestOrResponse => Unit = null,
                        var updateMetadataResponseCallback: RequestOrResponse => Unit = null,
                        var stopReplicaResponseCallback: (RequestOrResponse, Int) => Unit = null)