package hexagon.zookeeper

import hexagon.tools.Logging
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache, PathChildrenCacheEvent, PathChildrenCacheListener}
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type._


abstract class PathChildrenListener(val cache: PathChildrenCache) extends PathChildrenCacheListener with Logging {

  def this(zkClient: ZkClient, path: String, cacheData: Boolean) = this(zkClient.createPathChildrenCache(path, cacheData))

  def this(zkClient: ZkClient, path: String) = this(zkClient, path, false)

  def childEvent(client: CuratorFramework, event: PathChildrenCacheEvent): Unit = {
    val eventType = event.getType
    eventType match {
      case CHILD_UPDATED => onDataChange(event.getData)
    }
  }

  def onDataChange(data: ChildData): Unit

}
