package hexagon.network

private object Handler {

  type Handler = Receive => Option[Send]

  type HandlerMapping = (Short, Receive) => Handler

}
