package com.kenbritton

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.json._
import DefaultJsonProtocol._
import spray.http._
import HttpMethods._
import com.mongodb.casbah.Imports._
import HashidsJava._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class LinkServiceActor extends Actor with LinkService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(route)
}

// this trait defines our service behavior independently from the service actor
trait LinkService extends HttpService {
	
	val mongoClient =  MongoClient("localhost", 27017)
	val db = mongoClient("links")
	val hashColl = db("hashes")
	hashColl.ensureIndex(MongoDBObject("hash" -> 1))
	val hashids = new Hashids("ken.ly.super.secure.salt", 8);
	
	val route = {
		get {
			path("actions" / "hash") {
				parameter("url") { url =>
					
					val hash = hashids.encrypt(java.lang.Math.abs(url.hashCode))
					val count = 0: java.lang.Integer
					val doc = MongoDBObject()
					doc += "url" -> url
					doc += "hash" -> hash
					doc += "count" -> count
					hashColl.insert(doc)
					
					respondWithMediaType(`application/json`) { 
						complete {
							s"""{"originalURL":"${url}","shortenedURL":"ken.ly/${hash}"}"""
						}
					}
				}
			} ~
			path("actions" / "stats") {
				parameter("hash") { hash =>
					
					val doc = hashColl.findOne(MongoDBObject("hash" -> hash))
					doc match {
						case Some(doc) =>
							respondWithMediaType(`application/json`) { 
								complete {
									s"""{"shortenedURL":"ken.ly/${hash}","clickCount":"${doc.get("count")}"}"""
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
			path("[\\w\\d]{8,}".r) { hash =>
				val doc = hashColl.findOne(MongoDBObject("hash" -> hash))
				doc match {
					case Some(doc) =>
						hashColl.update(MongoDBObject("hash" -> hash), $inc("count" -> 1))
						redirect(doc.get("url").toString, StatusCodes.TemporaryRedirect)
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