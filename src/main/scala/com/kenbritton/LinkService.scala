package com.kenbritton

import akka.actor.Actor
import spray.routing._
import spray.http._
import MediaTypes._
import spray.json._
import DefaultJsonProtocol._
import HttpMethods._
import com.mongodb.casbah.Imports._
import com.mongodb.MongoException
import HashidsJava._
import java.net._
import scala.util.control.Exception._
import scala.util._

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

	def parseURL(url: String): Try[URL] = Try(new URL(url))
	def insertDoc(doc: MongoDBObject, coll: MongoCollection): Try[WriteResult] = Try(coll.insert(doc))
	
	// MongoDB connection
	val mongoClient =  MongoClient("localhost", 27017)
	val db = mongoClient("links")
	val hashColl = db("hashes")
	
	// ensure indexes are in place
	hashColl.ensureIndex(MongoDBObject("hash" -> 1))
	hashColl.ensureIndex(MongoDBObject("url" -> 1), MongoDBObject("unique" -> true))
	
	// Hash generator
	val hashids = new Hashids("ken.ly.super.secure.salt", 8);
	
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
							val hash = hashids.encrypt(java.lang.Math.abs(url.hashCode))
							val count = 0: java.lang.Integer
							val doc = MongoDBObject()
							doc += "url" -> url
							doc += "hash" -> hash
							doc += "count" -> count
							insertDoc(doc, hashColl)
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
					
					val doc = hashColl.findOne(MongoDBObject("hash" -> hash))
					doc match {
						case Some(doc) =>
							respondWithMediaType(`application/json`) { 
								complete {
									s"""{"hash":"${hash}","clickCount":"${doc.get("count")}"}"""
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