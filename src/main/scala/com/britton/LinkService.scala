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

/**
	Our akka actor!
*/
class LinkServiceActor extends Actor with LinkService {

	def actorRefFactory = context

	// this actor only runs our route, but you could add
	// other things here, like request stream processing
	// or timeout handling
	def receive = runRoute(route)
}

/**
 	Our routing trait.  Actors / tests with this trait can route HTTP requests.
*/
trait LinkService extends HttpService {

	//attempt to parse a URL (may fail with MalformedURLException)
	def parseURL(url: String): Try[URL] = Try(new URL(url))
	
	// Hash generator
	val hashGenerator = new Hashids("ken.ly.super.secure.salt", 8);
	
	// Persistence
	val dataStore = DataStoreFactory.getInstance();
	
	// routing tree for requests
	val route = {
		get {
			path("actions" / "hash") { // hash-generation
				parameter("url") { url =>
					val parsedURL = parseURL(url).map(_.getProtocol)
					parsedURL match {
						case Failure(ex) =>
							respondWithMediaType(`application/json`) { 
								complete {
									s"""{"error":"Invalid URL - ${ex.getMessage}"}"""
								}
							}
						case Success(protocol) => {
							val hash = hashGenerator.encrypt(java.lang.Math.abs(url.hashCode))
							dataStore.trackLink(url, hash, 0)
							respondWithMediaType(`application/json`) { 
								complete {
									s"""{"originalURL":"${url}","hash":"${hash}"}"""
								}
							}
						}
					}
				}
			} ~
			path("actions" / "stats") { // stats
				parameter("hash") { hash =>
					val doc = dataStore.findLink(hash)
					doc match {
						case Some(doc) =>
							respondWithMediaType(`application/json`) { 
								complete {
									s"""{"hash":"${hash}","clickCount":"${doc.count}"}"""
								}
							}
						case None =>
							respondWithMediaType(`application/json`) { 
								respondWithStatus(StatusCodes.NotFound) {
									complete {
										s"""{"error":"No stats available for the requested URL"}"""
									}
								}
							}
					
					}
				}
			} ~ 
			path("[\\w\\d]{8,}".r) { hash => // link processor
				val doc = dataStore.findLink(hash)
				doc match {
					case Some(doc) =>
						dataStore.incrementClicks(doc)
						redirect(doc.url, StatusCodes.MovedPermanently)
					case None =>
						respondWithStatus(StatusCodes.NotFound) {
							complete {
								"The requested URL could not be found"
							}
						}
				}
			}
		}
	}
}

// Factory for creating DataStore instances
object DataStoreFactory {
	def getInstance() : DataStore =
		Properties.envOrNone("MONGOLAB_URI") match {
			case Some(uri) => new MongoDataStore(uri)
			case None => new MemoryDataStore
		}
}