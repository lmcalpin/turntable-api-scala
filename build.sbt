organization := "com.metatrope"

name := "turntable-api-scala"

version := "1.0"

scalaVersion := "2.11.7"

// additional libraries
libraryDependencies ++= Seq(
    "org.specs2" %% "specs2" % "3.3.1" % "test", // For specs.org tests
	"org.scalatest" %% "scalatest" % "2.2.4", // scalatest
	"junit" % "junit" % "4.8" % "test->default", // For JUnit 4 testing
	"ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default", // Logging
  	"org.codehaus.groovy" % "groovy-all" % "1.7.5" % "runtime", 
  	"com.typesafe.akka" %% "akka-actor" % "2.3.2",
  	"net.liftweb" %% "lift-json" % "2.6-M4",
  	"org.scalaj" %% "scalaj-http" % "1.1.5",
    "org.scala-lang.modules" %% "scala-xml" % "1.0.2" 
)

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"