package hexagon.tools


/**
  * 自定义迭代器模板类
  */
abstract class ItrTemplate[T] extends Iterator[T] with java.util.Iterator[T] {

	private var state: State = NOT_READY

	private var nextItem: Option[T] = None

	/**
	  * 判断是否有下一个元素
	  */
	override def hasNext: Boolean = {
		if (state == FAILED)
			throw new IllegalStateException("Iterator is in failed state.")

		state match {
			case DONE => false
			case READY => true
			case _ => maybeComputeNext()
		}
	}

	/**
	  * 迭代获取下一个元素
	  */
	override def next(): T = {
		if (!hasNext())
			throw new NoSuchElementException()
		state = NOT_READY
		nextItem match {
			case Some(item) => item
			case None => throw new IllegalStateException("Expected item but none found.")
		}
	}


	/**
	  * 尝试获取下一个元素，用来支持maybeComputeNext()或next()方法。
	  *
	  * 当前模板类的核心方法
	  */
	protected def makeNext(): T


	/**
	  * 判断是否存在下一个元素，并尝试取出
	  */
	def maybeComputeNext(): Boolean = {
		state = FAILED
		nextItem = Some(makeNext())
		if (state == DONE) {
			false
		} else {
			state = READY
			true
		}
	}

	protected def done(): T = {
		state = DONE
		null.asInstanceOf[T]
	}

	protected def resetState(): Unit = {
		state = NOT_READY
	}

	override def remove(): Unit =
		throw new UnsupportedOperationException("Removal not supported")

}


sealed class State

case object DONE extends State

case object READY extends State

case object NOT_READY extends State

case object FAILED extends State
