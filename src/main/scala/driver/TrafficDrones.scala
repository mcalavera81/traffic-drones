package driver

import actor.Dispatcher
import akka.actor._



object TrafficDrones extends App {

  // Create the 'TrafficDrones' actor system
  val system = ActorSystem("TrafficDrones")


  //Create the dispatcher actor
  import Dispatcher._
  val dispatcher = system.actorOf(Dispatcher.props(),"dispatcher")

  dispatcher ! StartUpDispatcher


}

