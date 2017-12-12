package hexagon.utils

import java.util.Properties

object PropKit {

  def getInt(props: Properties, name: String, defaultValue: Int): Int = {
    if (props.containsKey(name))
      return props.getProperty(name).toInt
    return defaultValue
  }


  def getInt(props: Properties, name: String): Int = {
    return getInt(props, name, -1)
  }

  def getLong(props: Properties, name: String, defaultValue: Long): Long = {
    if (props.containsKey(name))
      return props.getProperty(name).toLong
    return defaultValue
  }


  def getLong(props: Properties, name: String): Long = {
    return getLong(props, name, -1L)
  }


  def getString(props: Properties, name: String, defaultValue: String): String = {
    if (props.containsKey(name))
      return props.getProperty(name)
    return defaultValue
  }


  def getString(props: Properties, name: String): String = {
    return getString(props, name, null)
  }

}
