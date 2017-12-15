package hexagon.utils

import org.I0Itec.zkclient.serialize.ZkSerializer

object ZKUtils {


  object ZkStringSerializer extends ZkSerializer {

    override def serialize(data: scala.Any): Array[Byte] = data.asInstanceOf[String].getBytes("UTF-8")

    override def deserialize(bytes: Array[Byte]): AnyRef = {
      if (null == bytes)
        null
      else
        new String(bytes, "UTF-8")
    }
  }


}
