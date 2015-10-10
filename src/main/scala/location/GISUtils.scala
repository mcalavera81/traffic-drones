package location

import actor.Common.Location
import actor.Drone.TubeLocation
import com.spatial4j.core.distance.DistanceUtils
import DistanceUtils._
/**
 * Created by mcalavera81 on 09/10/15.
 */
object GISUtils {
  //All distances in meters


  def distance(location1:Location, location2:Location):Double={

    val distance =distHaversineRAD(toRadians(location1.latitude),toRadians(location1.longitude),
        toRadians(location2.latitude),toRadians(location2.longitude))

    radians2Dist(distance, EARTH_MEAN_RADIUS_KM)*1000//meters

  }

  def tubeStationsWithinDistance(maxDist:Int,location: Location, tubeStations:List[TubeLocation]):List[(TubeLocation,Double)]={
    tubeStations.map{
      tubeStation=> (tubeStation, distance(tubeStation.location, location))
    }.filter{
      case (tubeStation,distanceToTubeStation)=>  distanceToTubeStation < maxDist
    }

  }

  def main(args: Array[String]) {
    val barcelona=Location(41.3887900,2.1589900)
    val london =Location(51.5085300,-0.1257400)

    println(distance(barcelona, london))
  }

}
