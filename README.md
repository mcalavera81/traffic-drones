# traffic-drones
Automatic drones that fly around London and report on traffic conditions.

Building:
>> sbt assembly

How to run:
>> java -Dapp.filesPath=/Users/mcalavera81/mag_java_test/ -jar target/scala-2.11/traffic-drones-assembly-1.0.jar
where app.filesPath is the path where the CSVs are kept.

