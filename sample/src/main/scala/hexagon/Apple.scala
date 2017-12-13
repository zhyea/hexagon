package hexagon

class Apple(val name: String) extends Fruit {


  def this() = this("apple")

  override def color(): String = "red"

}
