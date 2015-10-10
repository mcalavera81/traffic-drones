name := """traffic_drones"""

version := "1.0"

scalaVersion := "2.11.7"

lazy val akkaVersion = "2.4.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "junit" % "junit" % "4.12" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test",
  "joda-time" % "joda-time" % "2.8.2",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.spatial4j" % "spatial4j" % "0.5"
)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

mainClass in assembly := Some("driver.TrafficDrones")

fork in run := true