package hexagon.cluster

case class TopicAndReplica(topic: String, replica: Int) {

  override def toString: String = s"[Topic=$topic,Replica=$replica]"

}
