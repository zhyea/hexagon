package hexagon.serializer

import hexagon.protocol.Message


trait Serializer[T] {

	def toMessage(src: T): Message

}


class StringSerializer extends Serializer[String] {

	override def toMessage(src: String): Message = new Message(src.getBytes)

}
