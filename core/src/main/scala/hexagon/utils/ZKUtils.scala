package hexagon.utils

import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.exception.{ZkMarshallingError, ZkNoNodeException}
import org.I0Itec.zkclient.serialize.ZkSerializer

object ZKUtils {


	/**
	  * Create the parent path.
	  */
	private def createParentPath(zkClient: ZkClient, path: String): Unit = {
		val parentDir = path.substring(0, path.lastIndexOf('/'))
		if (parentDir.length != 0) {
			zkClient.createPersistent(parentDir, true)
		}
	}


	/**
	  * Create an ephemeral node with the given path and data. Create parents if necessary.
	  */
	private def createEphemeralPath(zkClient: ZkClient, path: String, data: String): Unit = {
		try {
			zkClient.createEphemeral(path, data)
		} catch {
			case e: ZkNoNodeException => {
				createParentPath(zkClient, path)
				zkClient.createEphemeral(path, data)
			}
		}
	}


	/**
	  * Create an ephemeral node with the given path and data.
	  * Throw NodeExistsException if node already exists.
	  */
	def createEphemeralPathExpectConflict(zkClient: ZkClient, path: String, data: String): Unit = {
		try {
			createEphemeralPath(zkClient, path, data)
		} catch {
			case
		}
	}


	/**
	  * ZooKeeper String Serializer.
	  */
	object ZkStringSerializer extends ZkSerializer {

		@throws(classOf[ZkMarshallingError])
		override def serialize(data: scala.Any): Array[Byte] = data.asInstanceOf[String].getBytes("UTF-8")

		@throws(classOf[ZkMarshallingError])
		override def deserialize(bytes: Array[Byte]): AnyRef = {
			if (null == bytes)
				null
			else
				new String(bytes, "UTF-8")
		}
	}

}
