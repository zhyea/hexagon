package hexagon.utils

import java.io.FileInputStream
import java.util.Properties


object PropKit {


  def load(path: String): Properties = {
    val props = new Properties()
    val in = new FileInputStream(path)
    try {
      props.load(in)
      props
    } finally {
      if (null != in) in.close()
    }
  }

  def getString(props: Properties, key: String, defaultValue: String = null): String = {
    if (props.containsKey(key))
      props.getProperty(key)
    else
      defaultValue
  }


  def getInt(props: Properties, key: String, defaultValue: Int = 0): Int = {
    if (props.containsKey(key))
      props.getProperty(key).toInt
    else
      defaultValue
  }


}
