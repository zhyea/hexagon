package hexagon.serializer


trait Serializer[T] {

	def serialize(src: T): Array[Byte]

}


class StringSerializer extends Serializer[String] {

	override def serialize(src: String): Array[Byte] = src.getBytes

}
