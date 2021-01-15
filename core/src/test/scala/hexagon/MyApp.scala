package hexagon


object MyApp extends App {

	val list = List((1, 'a'), (2, 'b'), (3, 'c'), (1, 'e'), (1, 'f'))

	val map = list.groupMapReduce(_._1)(_._2.toString)(_ ++ _)

	println(map)



}
