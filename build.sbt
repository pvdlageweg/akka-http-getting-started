scalaVersion := "2.13.3"

name := "getting-started"
organization := "com.pvdlageweg.akkahttp"
version := "1.0"

lazy val akkaVersion = "2.6.8"
lazy val akkaHttpVersion = "10.1.12"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
libraryDependencies +="com.typesafe.akka" %% "akka-stream" % akkaVersion
libraryDependencies +="com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
