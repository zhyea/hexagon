package hexagon.tools

import java.util.Properties


object PropKit {


  def getString(props: Properties, key: String, defaultValue: String): String = {
    if (props.containsKey(key))
      props.getProperty(key)
    else
      defaultValue
  }


  def getString(props: Properties, key: String): String = getString(props, key, null)


  def getInt(props: Properties, key: String, defaultValue: Int): Int = {
    if (props.containsKey(key))
      props.getProperty(key).toInt
    else
      defaultValue
  }


  def getInt(props: Properties, key: String): Int = getInt(props, key, 0)
}
