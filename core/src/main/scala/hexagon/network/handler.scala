package hexagon.network

import java.nio.channels.SelectionKey

private[hexagon] object handler {


  type ProcessorHandler = (SelectionKey, Receive) => Option[Send]


}
