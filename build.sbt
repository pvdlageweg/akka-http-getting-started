scalaVersion := "2.13.3"

name := "getting-started"
organization := "com.pvdlageweg.akkahttp"
version := "1.0"

lazy val akkaVersion = "2.6.8"

libraryDependencies += "com.typesafe.akka" %% "akka-http"   % "10.1.12"
libraryDependencies +="com.typesafe.akka" %% "akka-stream" % akkaVersion