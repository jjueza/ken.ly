package com.britton

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import util.Properties

/**
	Main entry point for our application.
*/
object Bootstrap extends App {
	
	// we need an ActorSystem to host our application in
	implicit val system = ActorSystem("on-spray-can")

	// create and start our service actor
	val service = system.actorOf(Props[LinkServiceActor], "demo-service")

	val listenPort = Properties.envOrElse("PORT", "8080").toInt

	// start a new HTTP server on port 8080 with our service actor as the handler
	IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = listenPort)
}