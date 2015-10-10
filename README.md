# traffic-drones
Automatic drones that fly around London and report on traffic conditions.

Building:
>> sbt assembly

How to run:
>> java -Dapp.filesPath=<absolute_path> -jar target/scala-2.11/traffic_drones-assembly-1.0.jar
where app.filesPath is the path where the CSVs are kept.

The output is located in the same path specified as per the app.filesPath variable.There is an
output file per drone.
For instance. For a drone with id 6000 the output would be:

|DroneIdTube |Station       |  Time                    |Speed(km/h)  |Traffic Condition|
|------------|--------------|--------------------------|-------------|-----------------|
|6000        |Pimlico       |    2011-03-22 07:59:04   | 36          |LIGHT            |
|6000        |Westminster   |    2011-03-22 08:04:12   | 32          |HEAVY            |
