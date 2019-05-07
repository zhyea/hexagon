/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package hexagon.network

import java.net._
import java.nio.ByteBuffer
import java.util.concurrent._

import hexagon.api.{RequestKeys, RequestOrResponse}
import hexagon.tools.{Logging, SysTime}


object RequestChannel extends Logging {


  case class Request(processor: Int,
                     requestKey: Any,
                     private var buffer: ByteBuffer,
                     startTimeMs: Long,
                     remoteAddress: SocketAddress = new InetSocketAddress(0)) {
    /**
      * 请求出队时间
      */
    @volatile var requestDequeueTimeMs: Long = -1L

    @volatile var apiLocalCompleteTimeMs: Long = -1L
    /**
      * 响应完成时间
      */
    @volatile var responseCompleteTimeMs: Long = -1L
    /**
      * 响应出队时间
      */
    @volatile var responseDequeueTimeMs: Long = -1L

    val requestId: Short = buffer.getShort()
    val requestObj: RequestOrResponse = RequestKeys.deserializerForKey(requestId)(buffer)
    buffer = null

    trace(s"Processor $processor received request : $requestObj")
  }


  case class Response(processor: Int,
                      request: Request,
                      responseSend: Send,
                      responseAction: ResponseAction) {

    request.responseCompleteTimeMs = SysTime.mills

    def this(processor: Int, request: Request, responseSend: Send) =
      this(processor, request, responseSend, if (null == responseSend) NoOpAction else SendAction)

    def this(request: Request, send: Send) = this(request.processor, request, send)
  }

  trait ResponseAction

  case object SendAction extends ResponseAction

  case object NoOpAction extends ResponseAction

  case object CloseConnectionAction extends ResponseAction

}

class RequestChannel(val numProcessors: Int, val queueSize: Int) extends Logging {

  private var responseListeners: List[Int => Unit] = Nil
  /**
    * 请求队列
    */
  private val requestQueue = new ArrayBlockingQueue[RequestChannel.Request](queueSize)
  /**
    * 响应队列，每个Processor一个相应队列
    */
  private val responseQueues = new Array[BlockingQueue[RequestChannel.Response]](numProcessors)

  for (i <- 0 until numProcessors) {
    responseQueues(i) = new LinkedBlockingQueue[RequestChannel.Response]()
  }


  /**
    * 将一个请求置入请求队列，如队列中没有空间将会阻塞
    */
  def sendRequest(request: RequestChannel.Request) {
    requestQueue.put(request)
  }

  /**
    * 将Response置入对应响应队列
    */
  def sendResponse(response: RequestChannel.Response) {
    responseQueues(response.processor).put(response)
    for (onResponse <- responseListeners)
      onResponse(response.processor)
  }

  /**
    * 对请求不作处理，需要继续获取更多数据
    */
  def noOperation(processor: Int, request: RequestChannel.Request) {
    responseQueues(processor).put(RequestChannel.Response(processor, request, null, RequestChannel.NoOpAction))
    for (onResponse <- responseListeners)
      onResponse(processor)
  }

  /**
    * 关闭请求连接
    */
  def closeConnection(processor: Int, request: RequestChannel.Request) {
    responseQueues(processor).put(RequestChannel.Response(processor, request, null, RequestChannel.CloseConnectionAction))
    for (onResponse <- responseListeners)
      onResponse(processor)
  }

  /**
    * 获取下一个request，如发生阻塞，则等待至超时
    */
  def receiveRequest(timeout: Long): RequestChannel.Request =
    requestQueue.poll(timeout, TimeUnit.MILLISECONDS)

  /**
    * 获取下一个request，直至取完
    */
  def receiveRequest(): RequestChannel.Request = requestQueue.take()

  /**
    * 获取响应数据
    */
  def receiveResponse(processor: Int): RequestChannel.Response = {
    val response = responseQueues(processor).poll()
    if (response != null)
      response.request.responseDequeueTimeMs = SysTime.mills
    response
  }

  /**
    * 添加响应监听
    */
  def addResponseListener(onResponse: Int => Unit) {
    responseListeners ::= onResponse
  }

  def shutdown() {
    requestQueue.clear()
  }
}

