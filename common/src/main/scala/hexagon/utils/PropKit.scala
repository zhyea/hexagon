package hexagon.utils

import java.io.{File, FileInputStream}
import java.util.Properties

import hexagon.exceptions.HexagonException


object PropKit {


  def load(path: String): Properties = {
    val file = new File(path)
    if (!file.exists()) {
      throw new HexagonException(s"Config file:$path dose't exists.")
    }
    val in = new FileInputStream(file)
    try {
      val props = new Properties()
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


  def getLong(props: Properties, key: String, defaultValue: Long = 0): Long = {
    if (props.containsKey(key))
      props.getProperty(key).toLong
    else
      defaultValue
  }


  def getPositiveLong(props: Properties, key: String, defaultValue: Long = 0): Long = {
    val value = getLong(props, key)
    if (value > 0) value else defaultValue
  }


  def getDouble(props: Properties, key: String, defaultValue: Double = 0): Double = {
    if (props.containsKey(key))
      props.getProperty(key).toDouble
    else
      defaultValue
  }


}
