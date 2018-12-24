package hexagon

import java.util.concurrent.CountDownLatch

object MyApp extends App {

  val latch = new CountDownLatch(2);


  new Thread(() => {
    (1 to 2).foreach(e => {
      println(s"start 0 ------------------------>>>>$e")
      latch.countDown()
      println(s"end   0 ------------------------>>>>$e")
    })
  }).start()

  new Thread(() => {
    (1 to 2).foreach(e => {
      println(s"start a ------------------------>>>>$e")
      //latch.countDown()
      println(s"end   a ------------------------>>>>$e")
    })
  }).start()

  latch.await()

  println("-------------------------end")

}
