organization := "com.metatrope"

name := "turntable-api-scala"

version := "1.0"

scalaVersion := "2.9.1"

// additional libraries
libraryDependencies ++= Seq(
	"org.specs2" %% "specs2" % "1.6.1" % "test", // For specs.org tests
	"org.scalatest" %% "scalatest" % "1.6.1", // scalatest
	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
	"ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default", // Logging
  	"org.scala-tools.testing" %% "scalacheck" % "1.9" % "test",
  	"org.codehaus.groovy" % "groovy-all" % "1.7.5" % "runtime", 
  	"net.liftweb" %% "lift-json" % "2.4-M5",
    "se.scalablesolutions.akka" % "akka-actor" % "1.2",
    "se.scalablesolutions.akka" % "akka-typed-actor" % "1.2",
    "se.scalablesolutions.akka" % "akka-amqp" % "1.2",
    "se.scalablesolutions.akka" % "akka-testkit" % "1.2",
    "se.scalablesolutions.akka" % "akka-actor" % "1.2" 
)