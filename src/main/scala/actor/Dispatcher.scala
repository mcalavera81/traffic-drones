package actor

import java.io.{FileWriter, FileOutputStream, PrintWriter, File}

import actor.Common.Location
import actor.Drone.{TrafficCondition, ShutDownDrone, TubeLocation}
import akka.actor._
import com.typesafe.config.ConfigFactory
import io.IOUtils
import io.IOUtils.DronePointWrapper
import org.joda.time.format.DateTimeFormat
import org.joda.time.{LocalTime, DateTime}

import scala.collection.mutable
import scala.util.Random
import scala.collection.mutable.{Map => MMap}

/**
 * Created by mcalavera81 on 09/10/15.
 */
class Dispatcher extends Actor with ActorLogging {

  import Dispatcher._

  val syncIds = MMap[String,Int]()
  val droneDataPoints = MMap[String,List[DronePoint]]()
  val trafficReports = MMap[String,List[TrafficReport]]()
  val output = MMap[String,PrintWriter]()

  var tubeLocations:List[TubeLocation] = _
  val random = Random

  var stopHour,stopMinute:Int= _
  var filesPath,tubeFileName:String = _





  override def preStart() {

    import Drone._
    configureDispatcher(this)

    val d = new File(filesPath)

    val files = d.listFiles.filter(file => file.isFile && file.getName.endsWith(".csv")).toList
    val (tubeFile, dronesFiles) = files.partition(file => file.getName.endsWith(tubeFileName))

    tubeLocations = IOUtils.readFile(tubeFile(0).getPath).asInstanceOf[List[TubeLocation]]

    dronesFiles.foreach(droneFile => {
      val dataPoints = IOUtils.readFile(droneFile.getPath).asInstanceOf[List[DronePointWrapper]]
      val droneId = dataPoints(0).droneId
      val drone = context.actorOf(Drone.props(self, droneId).withDispatcher("app.my-pinned-dispatcher"), droneId)
      syncIds += (droneId -> random.nextInt)
      droneDataPoints += (droneId -> dataPoints.map(_.droinPoint))
      output += (droneId -> IOUtils.fileWriter(filesPath, droneId))
      drone ! StartUpDrone(syncIds(droneId), tubeLocations)

    })

  }

  def receive = disabled

  def disabled: Receive = {
    case StartUpDispatcher => context.become(enabled)
    case _ => log.error("The dispatcher is disabled")
  }

  def enabled: Receive = {
    case DataPull(windowSize, droneId, syncId) => {

      val (pointsToSend, pointsToKeep) = droneDataPoints(droneId) splitAt windowSize
      droneDataPoints += (droneId -> pointsToKeep)

      def timeConditionToKeepAliveDrone(drone: DronePoint) = drone.time.toLocalTime.isBefore(new LocalTime(stopHour,stopMinute,0))

      syncIds += (droneId -> random.nextInt)
      if(!pointsToSend.exists(timeConditionToKeepAliveDrone)){
        sender ! ShutDownDrone
        droneDataPoints -= droneId
        syncIds -= droneId
      }else{
        sender ! Data(pointsToSend.takeWhile(dronePoint => timeConditionToKeepAliveDrone(dronePoint)), syncIds(droneId))
      }
      //log.info(s"Dispatcher sending ${pointsToSend.size} points to drone $droneId")

    }
    case AckDroneDown(droneId:String) => {
      output(droneId).close
      if(droneDataPoints.isEmpty) {
        log.info("Not any more drones alive. Shutting down the whole system")
        context.become(disabled)
        context.system.terminate()
      }
    }

    case tr @ TrafficReport(droneId, dronePoint,speed, trafficCondition, distance, tubeLocation) =>{

      val trafficReportList=trafficReports.getOrElse(droneId,List.empty[TrafficReport])
      trafficReports += (droneId-> (trafficReportList :+ tr))
      log.info(s"Drone $droneId at (${dronePoint.location.latitude},${dronePoint.location.longitude}) with speed ${IOUtils.speedStr(speed)}km/h" +
        s" has found this tube station: ${tubeLocation.station} at ${distance} meters at ${IOUtils.timeStr(dronePoint.time)}" )

      IOUtils.appendLine(output(droneId), droneId, dronePoint.time ,speed, trafficCondition, tubeLocation.station)

    }
    case _ =>

  }
}

object Dispatcher {

  def props(): Props = Props(classOf[Dispatcher])

  case object StartUpDispatcher

  case object ShutDownDispatcher

  case class AckDroneDown(droneId:String)

  case class DataPull(windowSize: Int, droneId: String, syncId: Int)

  case class DronePoint(location:Location, time: DateTime)

  case class Data(points: List[DronePoint], syncId: Int)

  case class TrafficReport(droneId:String, dronePoint: DronePoint, speed:Option[Double], trafficCondition: TrafficCondition,  distance:Int,
                            tubeLocation: TubeLocation)

  def configureDispatcher(dispatcher: Dispatcher)={
    val config = ConfigFactory.load()

    dispatcher.stopHour = config.getInt("app.drone.stop.hour")
    dispatcher.stopMinute = config.getInt("app.drone.stop.minute")
    dispatcher.filesPath = config.getString("app.filesPath")
    dispatcher.tubeFileName = config.getString("app.tubeFileName")

  }
}

