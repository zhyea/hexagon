package hexagon.network

import java.nio.channels.SelectionKey

private[hexagon] object Handler {


  type ProcessorHandler = (SelectionKey, Receive) => Option[Send]


}
