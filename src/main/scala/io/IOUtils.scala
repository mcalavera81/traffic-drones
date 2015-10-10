package io

import java.io.{FileWriter, PrintWriter, File}

import actor.Common.Location
import actor.Dispatcher.DronePoint
import actor.Drone
import actor.Drone.{TrafficCondition, TubeLocation}


import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.io.Source



/**
 * Created by mcalavera81 on 09/10/15.
 */
object IOUtils {


  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  case class DronePointWrapper(droneId: String, droinPoint:DronePoint)

  trait Mapper[M]{
    def rehydrate(array:Array[String]):M
  }



  object Mapper{


    implicit object TubeMapper extends Mapper[TubeLocation]{
      override def rehydrate(array: Array[String]): TubeLocation = {
        TubeLocation(array(0),Location(array(1).toFloat,array(2).toFloat))
      }
    }

    implicit object DronePointMapper extends Mapper[DronePointWrapper]{
      override def rehydrate(array: Array[String]): DronePointWrapper = {
        DronePointWrapper( array(0),
          DronePoint(Location(array(1).toFloat,array(2).toFloat),formatter.parseDateTime(array(3)))
        )
      }
    }

    def convert[E](array: Array[String])(implicit mapper: Mapper[E]):E={
      mapper.rehydrate(array)
    }

  }


  val outputFormatting = "%-8s %-20s %-23s %-12s %-8s\n"
  def fileWriter(filesPath:String,droneId:String):PrintWriter={
    val writer= new PrintWriter(new FileWriter(new File(s"$filesPath/$droneId.out"),false))
    writer.write(outputFormatting.format("DroneId", "Tube Station","Time", "Speed(km/h)", "Traffic Condition"))
    writer
  }

  def timeStr(dateTime: DateTime):String={
    formatter.print(dateTime)
  }

  def speedStr(speed:Option[Double]):String={
    speed.map(_.toInt.toString).getOrElse("NA")
  }

  def appendLine(writer:PrintWriter, droneId:String, time:DateTime,speed:Option[Double], trafficCondition: TrafficCondition,
                  tubeStation:String)={
    writer.append(outputFormatting.format(droneId, tubeStation, timeStr(time), speedStr(speed),trafficCondition))
  }

  def readFile(path:String)={
    import Mapper._


    val bufferedSource = Source.fromFile(path)
    val lines= (for (line <- bufferedSource.getLines)
      yield {
        val array = line.split(",").map(_.trim.replaceAll("\"", ""))
        if (path.endsWith("tube.csv")) {

          convert[TubeLocation](array)
        } else {
          convert[DronePointWrapper](array)
        }

      }).toList

    bufferedSource.close
    lines.toList
  }

  def main(args: Array[String]) {
    val d = new File("/Users/mcalavera81/Downloads/mag_java_test")

    val files = d.listFiles.filter(file => file.isFile &&  file.getName.endsWith(".csv")).toList
    val (tubeFile, dronesFile)= files.partition(file=> file.getName.endsWith("tube.csv"))


    println("*************TUBE**************")

    tubeFile.foreach(file=> {
      println(file.getPath)
      val tubeLocations=readFile(file.getPath).asInstanceOf[List[TubeLocation]]
      tubeLocations.foreach(println)
    })

    println("*************DRONES**************")

    dronesFile.foreach(droneFile=>{
      val dronePoints=readFile(droneFile.getPath).asInstanceOf[List[DronePointWrapper]]
      println("\n"*5)
      println(droneFile.getPath)
      dronePoints.foreach(println)
    } )




  }
}
