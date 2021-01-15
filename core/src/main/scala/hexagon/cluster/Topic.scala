package hexagon.cluster

case class Topic(topicName: String, brokerId: Int) {

	override def toString: String = s"$topicName, $brokerId"

}
