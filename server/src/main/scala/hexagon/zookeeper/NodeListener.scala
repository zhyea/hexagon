package hexagon.zookeeper

import hexagon.tools.Logging
import org.apache.curator.framework.recipes.cache.{ChildData, NodeCache, NodeCacheListener}

abstract class NodeListener(cache: NodeCache) extends NodeCacheListener with Logging {

  def this(path: String, zkClient: ZkClient) = this(zkClient.createNodeCache(path))


  override def nodeChanged(): Unit = {
    val data = cache.getCurrentData
    if (null == data) onNodeDelete()
    else onDataChange(data)
  }

  def onDataChange(data: ChildData): Unit


  def onNodeDelete(): Unit


}
