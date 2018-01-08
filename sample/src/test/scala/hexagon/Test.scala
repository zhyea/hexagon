package hexagon

import java.util.Properties

object Test extends App {

  val props = new Properties()

  props.put("zz", null)

  println(props.get("zz").toString)


}

