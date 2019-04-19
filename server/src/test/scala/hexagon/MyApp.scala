package hexagon

import hexagon.tools.Logging


object MyApp extends App with Logging {

  val path = "/ab/c/d/"

  val p = if (path.endsWith("/")) path.substring(0, path.length - 1) else path
  println(p)


}
