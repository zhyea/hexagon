package hexagon.utils

import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.exception.{ZkMarshallingError, ZkNoNodeException, ZkNodeExistsException}
import org.I0Itec.zkclient.serialize.ZkSerializer

import scala.collection.JavaConverters._

object ZKUtils extends Logging {


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
			case e: ZkNodeExistsException => {
				var storeDate: String = null
				try {
					storeDate = readData(zkClient, path)
				} catch {
					case e: ZkNoNodeException =>
					case e2 => throw e2
				}
				if (null == storeDate || storeDate != data) {
					info("Conflict in path:{}, data:{}, store data:{}", path, data, storeDate)
					throw e
				}
				info("{} exists with value {} during connection loss; this is ok.")
			}
			case e2 => throw e2
		}
	}


	/**
	  * Delete the given path.
	  */
	def deletePath(zkClient: ZkClient, path: String): Unit = {
		try {
			zkClient.delete(path)
		} catch {
			case e: ZkNoNodeException => {
				info("{} deleted during connection loss.", path)
			}
			case e2 => throw e2
		}
	}


	/**
	  * Delete the given path recursively.
	  */
	def deletePathRecursive(zkClient: ZkClient, path: String): Unit = {
		try {
			zkClient.deleteRecursive(path)
		} catch {
			case e: ZkNoNodeException => {
				info("{} deleted during connection loss.", path)
			}
			case e2 => throw e2
		}
	}


	/**
	  * Read data of the given node.
	  */
	def readData(zkClient: ZkClient, path: String): String = {
		zkClient.readData(path)
	}


	/**
	  * Read data of the given node. If the path doesn't exists, will return null.
	  */
	def readDataMaybeNull(zkClient: ZkClient, path: String): String = {
		zkClient.readData(path, true)
	}


	/**
	  * Get child nodes of the given node.
	  */
	def getChildren(zkClient: ZkClient, path: String): Seq[String] = {
		zkClient.getChildren(path).asScala
	}


	/**
	  * Get child nodes of the given node. If the given doesn't exists, will return an empty Seq.
	  */
	def getChildrenParentMayNotExist(zkClient: ZkClient, path: String): Seq[String] = {
		var result: java.util.List[String] = null
		try {
			result = zkClient.getChildren(path)
		} catch {
			case e: ZkNoNodeException => {
				return Nil
			}
			case e2 => throw e2
		}
		result.asScala
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

