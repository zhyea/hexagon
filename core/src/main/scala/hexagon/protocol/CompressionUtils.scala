package hexagon.protocol

import java.io.{ByteArrayOutputStream, IOException, InputStream}
import java.nio.ByteBuffer
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import hexagon.exceptions.UnknownCodecException
import hexagon.tools.Logging


abstract sealed class CompressionFacade(input: InputStream, output: ByteArrayOutputStream) {

	def read(arr: Array[Byte]): Int

	def write(arr: Array[Byte]): Unit

	def close(): Unit = {
		if (null != input) input.close()
		if (null != output) output.close()
	}
}


class GZIPCompression(input: InputStream, output: ByteArrayOutputStream) extends CompressionFacade(input, output) {


	val gzipIn: GZIPInputStream = if (null == input) null else new GZIPInputStream(input)
	val gzipOut: GZIPOutputStream = if (null == output) null else new GZIPOutputStream(output)


	override def close(): Unit = {
		if (null != gzipIn) gzipIn.close()
		if (null != gzipOut) gzipOut.close()
		super.close()
	}

	override def read(arr: Array[Byte]): Int = {
		gzipIn.read(arr)
	}

	override def write(arr: Array[Byte]): Unit = {
		gzipOut.write(arr)
	}
}


object CompressionFactory {

	def apply(compressionCodec: CompressionCodec, input: InputStream): CompressionFacade =
		compressionCodec match {
			case GZIPCompressionCodec => new GZIPCompression(input, null)
			case _ => throw new UnknownCodecException(s"Unknown Codec: $compressionCodec")
		}


	def apply(compressionCodec: CompressionCodec, output: ByteArrayOutputStream): CompressionFacade =
		compressionCodec match {
			case GZIPCompressionCodec => new GZIPCompression(null, output)
			case _ => throw new UnknownCodecException(s"Unknown Codec: $compressionCodec")
		}

}


object CompressionUtils extends Logging {

	def compress(entities: Iterable[Entity], compressionCodec: CompressionCodec = DefaultCompressionCodec): Entity = {

		val output: ByteArrayOutputStream = new ByteArrayOutputStream()

		debug(s"Allocating entity byte buffer of size = ${EntitySet.entitySetSize(entities)}")

		val cf: CompressionFacade = CompressionFactory(compressionCodec, output)

		val buffer = ByteBuffer.allocate(EntitySet.entitySetSize(entities))
		entities.foreach(_.serializeTo(buffer))
		buffer.rewind()

		try {
			cf.write(buffer.array())
		} catch {
			case e: IOException =>
				error("Error while while writing to the GZIP output stream", e)
				throw e
		} finally {
			cf.close()
		}

		new Entity(output.toByteArray, compressionCodec)
	}


	def decompress(entity: Entity): ByteBufferEntitySet = {
		val output: ByteArrayOutputStream = new ByteArrayOutputStream
		val input: InputStream = new ByteBufferBackedInputStream(entity.payload)

		val cf: CompressionFacade = CompressionFactory(entity.compressionCodec, input)

		val intermediateBuffer = new Array[Byte](1024)
		try {
			LazyList.continually(cf.read(intermediateBuffer))
				.takeWhile(_ > 0)
				.foreach {
					output.write(intermediateBuffer, 0, _)
				}
		} catch {
			case e: IOException =>
				error("Error while reading from the GZIP input stream", e)
				throw e
		} finally {
			cf.close()
		}

		val buffer = ByteBuffer.allocate(output.size)
		buffer.put(output.toByteArray)
		buffer.rewind

		new ByteBufferEntitySet(buffer)
	}
}
