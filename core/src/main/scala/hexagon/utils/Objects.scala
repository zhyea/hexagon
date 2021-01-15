package hexagon.utils

object Objects {


	def getObject[T <: AnyRef](className: String): T = {
		className match {
			case null => null.asInstanceOf[T]
			case _ =>
				val clazz = Class.forName(className)
				val clazzT = clazz.asInstanceOf[Class[T]]
				val constructors = clazzT.getConstructors
				require(constructors.length == 1)
				constructors.head.newInstance().asInstanceOf[T]
		}
	}

}
