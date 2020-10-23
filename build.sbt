scalaVersion := "2.13.3"

name := "getting-started"
organization := "com.pvdlageweg.akkahttp"
version := "1.0"

lazy val akkaVersion = "2.6.10"
lazy val akkaHttpVersion = "10.2.1"
lazy val slickVersion = "3.3.3"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies +="com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-persistence" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion
libraryDependencies +="com.typesafe.slick" %% "slick" % slickVersion
libraryDependencies +="com.typesafe.slick" %% "slick-hikaricp" % slickVersion
libraryDependencies += "com.lightbend.akka" %% "akka-persistence-jdbc" % "4.0.0"
libraryDependencies +="org.flywaydb" % "flyway-core" % "7.0.4"
libraryDependencies +="com.h2database" % "h2" % "1.4.200"
libraryDependencies +="org.postgresql" % "postgresql" % "42.2.18"
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
