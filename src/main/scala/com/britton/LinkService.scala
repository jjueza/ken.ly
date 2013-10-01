package com.britton

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.json._
import DefaultJsonProtocol._
import HttpMethods._
import HashidsJava._
import java.net._
import scala.util.control.Exception._
import scala.util._
import akka.actor.{ ActorContext, TypedActor, TypedProps, ActorSystem, Props }
import akka.util.Timeout
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

/**
	Our akka actor!
*/
class LinkServiceActor extends Actor with LinkService {

	def actorRefFactory = context
	def receive = runRoute(route)
}

/**
 	Our routing trait.  Actors / tests with this trait can route HTTP requests.
*/
trait LinkService extends HttpService {

	// URL parsing function (may fail with MalformedURLException)
	def parseURL(url: String): Try[URL] = Try(new URL(url))
	
	// Hash generator
	//   This generator will create hashes of at least 8 characters 
	//   in a style very similar to bit.ly or owl.ly.  This generator
	//   will always generate the same hash for the same URL.  If the user
	//   of this software wished to have unique URLs for every request, we 
	//   could hash a GUID instead of the original URL
	//
	//   Hashids GitHub project: https://github.com/peet/hashids.java
	val hashGenerator = new Hashids("ken.ly.super.secure.salt", 8);
	
	// Persistence
	val dataStore = DataStoreFactory.getInstance();
	
	val hashingService = path("actions" / "hash") {
		parameter("url") { url =>
			respondWithMediaType(`application/json`) {
				parseURL(url).map(_.getProtocol) match {
					case Success(protocol) => {							
						val hash = hashGenerator.encrypt(java.lang.Math.abs(url.hashCode))
						Try(dataStore.saveLink(url, hash, 0)) match {
							case Success(result) => 
								complete { s"""{"originalURL":"${url}","hash":"${hash}"}""" }
							case Failure(saveException) => 
								respondWithStatus(StatusCodes.InternalServerError) {
									complete { s"""{"error":"Could not save link to database: ${saveException.getMessage}"}""" }
								}
							}
						}
					case Failure(urlException) =>
						respondWithStatus(StatusCodes.BadRequest) {
							complete { s"""{"error":"Invalid URL - ${urlException.getMessage}"}""" }
						}
				}
			}
		}
	}
	
	val statsService = path("actions" / "stats") {
		parameter("hash") { hash =>
			respondWithMediaType(`application/json`) { 
				Try(dataStore.findLink(hash)) match {
					case Success(option) => {
						option match {
							case Some(doc) =>
								complete { s"""{"hash":"${hash}","clickCount":"${doc.count}"}""" }
							case None => 
								respondWithStatus(StatusCodes.NotFound) {
									complete { s"""{"error":"No stats available for the requested URL"}""" }
								}
						}
					}
					case Failure(findException) => {
						respondWithStatus(StatusCodes.InternalServerError) {
							complete { s"""{"error":"Could not find link: ${findException.getMessage}"}""" }
						}
					}
				}
			}
		}
	}
	
	val redirectService = path("[\\w\\d]{8,}".r) { hash =>
	 	Try(dataStore.findLink(hash)) match {
			case Success(option) => 
				option match {
					case Some(doc) =>
						Try(dataStore.incrementClicks(doc.hash)) match {
							case Success(newCount) => 
								redirect(doc.url, StatusCodes.MovedPermanently)
							case Failure(incException) => 
								respondWithStatus(StatusCodes.InternalServerError) {
									complete { s"""{"error":"Could not increment click count: ${incException.getMessage}"}""" }
								}
						}
					case None =>
						respondWithStatus(StatusCodes.NotFound) {
							complete { s"""{"error":"Link not found"}""" }
						}
				}
			case Failure(findException) =>
				respondWithStatus(StatusCodes.InternalServerError) {
					complete { s"""{"error":"Could not find link: ${findException.getMessage}"}""" }
				}
		}
	}
	
	// routing tree for requests. see spray-routing (http://spray.io/documentation/1.1-M8/spray-routing/) 
	// for more information about the directives used here.
	val route = { 
		get { 
			hashingService ~ statsService ~ redirectService 
		} 
	}
}

// Factory for creating DataStore instances
//   If the MONGOLAB_URI environment variable has been set, create a MongoDB data-store
//   otherwise create an in-memory store
object DataStoreFactory {
	
	val system = ActorSystem("on-spray-can")
	
	def getInstance() : DataStore =
		Properties.envOrNone("MONGOLAB_URI") match {
			case Some(uri) => 
				val props = TypedProps(classOf[DataStore], new MongoDataStore(uri)).withTimeout(Timeout(5, SECONDS))
				TypedActor(system).typedActorOf(props, "mongoDataStore")
			case None => 
				val props = TypedProps[MemoryDataStore]().withTimeout(Timeout(5, SECONDS))
				TypedActor(system).typedActorOf(props)
		}
}