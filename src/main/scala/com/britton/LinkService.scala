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
import akka.actor.{ ActorContext, TypedActor, TypedProps, ActorSystem }
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
	
	// time to wait for futures
	val timeout = Duration(5, SECONDS);
	
	// routing tree for requests. see spray-routing (http://spray.io/documentation/1.1-M8/spray-routing/) 
	// for more information about the directives used here.
	val route = {
		get {
			path("actions" / "hash") {
				parameter("url") { url =>
					parseURL(url).map(_.getProtocol) match {
						case Failure(ex) =>
							respondWithMediaType(`application/json`) { 
								complete { s"""{"error":"Invalid URL - ${ex.getMessage}"}""" }
							}
						case Success(protocol) => {
							respondWithMediaType(`application/json`) {								
								val hash = hashGenerator.encrypt(java.lang.Math.abs(url.hashCode))
								val fut = dataStore.trackLink(url, hash, 0)
								Try(Await.result(fut, timeout)) match {
									case Success(result) => complete { s"""{"originalURL":"${url}","hash":"${hash}"}""" }
									case Failure(ex) => complete { s"""{"error":"Could not save link to database:${ex.getMessage}"}""" }
								}
							}
						}
					}
				}
			} ~
			path("actions" / "stats") {
				parameter("hash") { hash =>
					val fut = dataStore.findLink(hash)
					Try(Await.result(fut, timeout)) match {
						case Success(doc) => {
							respondWithMediaType(`application/json`) { 
								complete { s"""{"hash":"${hash}","clickCount":"${doc.count}"}""" }
							}
						}
						case Failure(ex) => {
							respondWithMediaType(`application/json`) { 
								respondWithStatus(StatusCodes.NotFound) {
									complete { s"""{"error":"No stats available for the requested URL"}""" }
								}
							}
						}
					}
				}
			} ~ 
			path("[\\w\\d]{8,}".r) { hash => // link processor
				val fut = dataStore.findLink(hash)
			 	Try(Await.result(fut, timeout)) match {
					case Success(doc) => 
						val fut = dataStore.incrementClicks(doc.hash)
						Try(Await.result(fut, timeout)) match {
							case Success(newCount) => redirect(doc.url, StatusCodes.MovedPermanently)
							case Failure(ex) => complete { s"""{"error":"Could not increment click count: ${ex.getMessage}"}""" }
						}
					case Failure(ex) =>
						respondWithStatus(StatusCodes.NotFound) {
							complete { "The requested URL could not be found" }
						}
				}
			}
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
				TypedActor(system).typedActorOf(TypedProps(classOf[DataStore], new MongoDataStore(uri)), "mongoDataStore")
			case None => 
				TypedActor(system).typedActorOf(TypedProps[MemoryDataStore]())
		}
}