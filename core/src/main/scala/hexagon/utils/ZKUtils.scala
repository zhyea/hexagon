package hexagon.utils

import org.I0Itec.zkclient.ZkClient
import org.I0Itec.zkclient.exception.ZkNoNodeException
import org.I0Itec.zkclient.serialize.ZkSerializer

import scala.collection.JavaConverters._

object ZKUtils extends Logging {


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


object ZkStringSerializer extends ZkSerializer {

  override def serialize(data: scala.Any): Array[Byte] = data.asInstanceOf[String].getBytes("UTF-8")

  override def deserialize(bytes: Array[Byte]): AnyRef = {
    if (null == bytes)
      null
    else
      new String(bytes, "UTF-8")
  }
}