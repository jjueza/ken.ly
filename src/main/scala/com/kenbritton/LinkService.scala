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
	
	val route = {
		get {
			path("actions" / "hash") {
				parameter("url") { url =>
					respondWithMediaType(`application/json`) { 
						
						val hash = "12345" //"%d".format(url.hashCode)
						val count = 0: java.lang.Integer
						val doc = MongoDBObject()
						doc += "url" -> url
						doc += "hash" -> hash
						doc += "count" -> count
						hashColl.save(doc)
						
						complete {
							s"""{"originalURL":"${url}","shortenedURL":"ken.ly/${hash}"}"""
						}
					}
				}
			} ~
			path("actions" / "stats") {
				parameter("hash") { hash =>
					respondWithMediaType(`application/json`) { 
						val count = hashColl.findOne(MongoDBObject("hash" -> hash)).get("count")
						complete {
							s"""{"shortenedURL":"ken.ly/${hash}","clickCount":"${count}"}"""
						}
					}
				}
			} ~ 
			path("[\\w\\d]{5}".r) { hash =>
				val url = hashColl.findOne(MongoDBObject("hash" -> hash)).get("url")
				println(url.toString)
				redirect(url.toString, StatusCodes.TemporaryRedirect)
			}
		}
	}
}