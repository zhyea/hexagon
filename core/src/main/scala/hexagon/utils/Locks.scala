package hexagon.utils

import java.util.concurrent.locks.{Lock, ReadWriteLock}

object Locks {


  def inLock[T](lock: Lock)(func: => T): T = {
    lock.lock()
    try {
      func
    } finally {
      lock.unlock()
    }
  }


  def inReadLock[T](lock: ReadWriteLock)(func: => T): T = inLock[T](lock.readLock())(func)


  def inWriteLock[T](lock: ReadWriteLock)(func: => T): T = inLock[T](lock.writeLock())(func)


  def sync[T](lock: Object)(func: => T): T = {
    lock synchronized {
      func
    }
  }

}
