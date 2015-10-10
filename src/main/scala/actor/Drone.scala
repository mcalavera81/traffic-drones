package actor

import actor.Common.Location
import actor.Drone.{StartUpDrone, TubeLocation}
import akka.actor._
import com.spatial4j.core.distance.DistanceUtils
import com.typesafe.config.ConfigFactory
import io.IOUtils
import location.GISUtils
import org.joda.time.{Seconds, Period, DateTime}

import scala.util.Random

/**
 * Created by mcalavera81 on 09/10/15.
 */
class Drone(dispatcher:ActorRef, droneId: String, initialBufferSize: Int, maxTubeStationDistance:Int)
  extends Actor with ActorLogging{


  var artificialDelay:Int = _

  var tubeStations:List[TubeLocation] = _
  import Drone._
  import Dispatcher._

  var bufferSize = initialBufferSize
  var lastLocationTimeSpeed:(Option[Location], Option[DateTime],Option[Double]) = (None,None,None)


  override def preStart() {
    configureDrone(this)
  }

  var tubeStationsReported:Set[String] = Set()


  def receive = disabled


  def disabled:Receive = {
    case StartUpDrone(batchId, tubeStations: List[TubeLocation]) => {
      context.become(enabled)
      this.tubeStations =tubeStations
      log.info(s"Starting up Drone $droneId")
      dispatcher ! DataPull(bufferSize,droneId,batchId)
      bufferSize = 0
    }
    case _ => log.error(s"The drone $droneId is disabled")
  }

  def enabled:Receive = {

    case Data(points,batchId) => {
      for ((point, index) <- points.zipWithIndex) {
        //log.info(s"Drone $droneId has received point: $point for batchId:$batchId, processing...")
        val speed =getSpeed(lastLocationTimeSpeed._1, lastLocationTimeSpeed._2,lastLocationTimeSpeed._3, point.location, point.time)
        lastLocationTimeSpeed = (Some(point.location),Some(point.time), speed)

        Thread.sleep(artificialDelay)

        val nearbyTubeStations = GISUtils.tubeStationsWithinDistance(maxTubeStationDistance, point.location, tubeStations)
        nearbyTubeStations.foreach(tubeStation=>{
          if(!tubeStationsReported.contains(tubeStation._1.station)){
            sender ! TrafficReport(droneId,point,speed,TrafficCondition.getNext(), tubeStation._2.toInt, tubeStation._1)
            tubeStationsReported += tubeStation._1.station
          }
        })

        bufferSize += 1
        if ((index + 1) % 5 == 0) {
          dispatcher ! DataPull(bufferSize, droneId, batchId)
          bufferSize = 0
        }

      }

    }
    case ShutDownDrone =>{
      log.info(s"Shutting down drone $droneId. Summary: Tube Stations found: $tubeStationsReported")

      context.stop(self)
      sender() ! AckDroneDown(droneId)
    }
    case ReceiveTimeout =>{

    }
  }



}

object Drone {

  case class StartUpDrone(batchId:Int, tubeStation:List[TubeLocation])

  case object ShutDownDrone

  object TrafficCondition{
    val random = new Random()
    def getNext():TrafficCondition={
      random.nextInt(3) match{
        case 0 => LIGHT
        case 1 => MODERATE
        case 2 => HEAVY
      }
    }
  }
  sealed trait TrafficCondition
  case object HEAVY extends TrafficCondition
  case object LIGHT extends TrafficCondition
  case object MODERATE extends TrafficCondition



  case class TubeLocation(station: String, location: Location)

  def props(dispatcher:ActorRef, droneId: String, msgBuffer: Int = 10, tubeStationDistance:Int=350): Props =
    Props(classOf[Drone], dispatcher,droneId, msgBuffer, tubeStationDistance)


  def configureDrone(drone: Drone)={
    val config = ConfigFactory.load()

    drone.bufferSize = config.getInt("app.drone.buffer.size")
    drone.artificialDelay = config.getInt("app.drone.artificialDelay")

  }


  def getSpeed(startLocation:Option[Location], startTime:Option[DateTime], oldSpeed:Option[Double],
               endLocation:Location, endTime:DateTime): Option[Double] ={



    startLocation.flatMap(sLoc=> startTime.flatMap(sTime=> {
      val period = new Period(sTime,endTime)
      if (period.toStandardSeconds.getSeconds==0){
        oldSpeed
      }else{
        Some(GISUtils.distance(sLoc, endLocation)/period.toStandardSeconds.getSeconds*0.001*3600) // Km/h
      }
    }))


  }
}