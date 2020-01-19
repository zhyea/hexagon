package hexagon.model

case class TopicAndPartition(topic: String, partition: Int) {


	override def toString: String = s"$topic,$partition"

}
