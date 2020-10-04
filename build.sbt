scalaVersion := "2.13.3"

name := "getting-started"
organization := "com.pvdlageweg.akkahttp"
version := "1.0"

lazy val akkaVersion = "2.6.9"
lazy val akkaHttpVersion = "10.2.1"
lazy val slickVersion = "3.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies +="com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies +="com.typesafe.slick" %% "slick" % slickVersion
libraryDependencies +="com.typesafe.slick" %% "slick-hikaricp" % slickVersion
libraryDependencies +="org.flywaydb" % "flyway-core" % "7.0.0"
libraryDependencies +="com.h2database" % "h2" % "1.4.200"
libraryDependencies +="com.typesafe.akka" %% "akka-slf4j" % akkaVersion
libraryDependencies +="ch.qos.logback" % "logback-classic" % "1.2.3"

scalacOptions ++= Seq(
  "-encoding", "utf-8",
  "-Xfatal-warnings",
  "-explaintypes",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-Ywarn-unused:imports"
)
