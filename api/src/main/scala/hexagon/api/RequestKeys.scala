package hexagon.api

import java.nio.ByteBuffer
import hexagon.exceptions.HexagonException

object RequestKeys {

  val Put: Short = 1

  val Query: Short = 2

  val MultiPut: Short = 3

  val MultiQuery: Short = 4

  val LeaderAndIsr: Short = 5

  val StopReplica: Short = 6

  val UpdateMetadata: Short = 7


  val keyToDeserializerMap: Map[Short, ByteBuffer => RequestOrResponse] =
    Map(
      Put -> PutRequest.readFrom,
      Query -> QueryRequest.readFrom,
      MultiPut -> MultiPutRequest.readFrom,
      MultiQuery -> MultiQueryRequest.readFrom,
      LeaderAndIsr -> LeaderAndIsrRequest.readFrom,
      StopReplica -> StopReplicaRequest.readFrom,
      UpdateMetadata -> UpdateMetadataRequest.readFrom,
    )


  def deserializerForKey(key: Short): ByteBuffer => RequestOrResponse = {
    keyToDeserializerMap.get(key) match {
      case Some(serializer) => serializer
      case None => throw new HexagonException(s"Wrong request type $key")
    }
  }

}
