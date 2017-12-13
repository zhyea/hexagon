package hexagon

import junit.framework.TestCase

class FruitTest extends TestCase {


  def testPrint() = {
    val apple: Fruit = new Apple("")
    print(apple)
  }

}
