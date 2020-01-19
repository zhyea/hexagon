package hexagon.tools

import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._

class Pool[K, V] extends Iterable[(K, V)] {

	private val pool = new ConcurrentHashMap[K, V]()

	def this(m: Map[K, V]) = {
		this()
		for ((k, v) <- m)
			pool.put(k, v)
	}


	def put(k: K, v: V): V = pool.put(k, v)

	def putIfNotExists(k: K, v: V): V = pool.putIfAbsent(k, v)

	def contains(id: K): Boolean = pool.containsKey(id)

	def get(key: K): V = pool.get(key)

	def remove(key: K): V = pool.remove(key)

	def clear(): Unit = pool.clear()

	def keys(): Iterable[K] = pool.keySet().asScala

	def values(): Iterable[V] = pool.values().asScala

	override def size: Int = pool.size

	/**
	  * Iterator
	  */
	override def iterator: Iterator[(K, V)] = new Iterator[(K, V)]() {

		private val itr = pool.entrySet.iterator

		override def hasNext: Boolean = itr.hasNext

		override def next(): (K, V) = {
			val n = itr.next()
			(n.getKey, n.getValue)
		}
	}


}
