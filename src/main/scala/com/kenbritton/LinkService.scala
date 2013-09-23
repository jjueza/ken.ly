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

class LinkServiceActor extends Actor with LinkService {

	def actorRefFactory = context

	// this actor only runs our route, but you could add
	// other things here, like request stream processing
	// or timeout handling
	def receive = runRoute(route)
}

// this trait defines our service behavior independently from the service actor
trait LinkService extends HttpService {

	//attempt to parse a URL (may fail with MalformedURLException)
	def parseURL(url: String): Try[URL] = Try(new URL(url))
	
	//attempt to insert a document to Mongo (may fail with MongoException.DuplicateKey)
	def insertDoc(doc: MongoDBObject, coll: MongoCollection): Try[WriteResult] = Try(coll.insert(doc))
	
	// MongoDB collection
	val mongoURI = MongoClientURI(Properties.envOrElse("MONGOLAB_URI", "mongodb://localhost:27017/links"))
	val mongoHashCollection =  MongoClient(mongoURI)(mongoURI.database.getOrElse(""))("hashes")
	
	// ensure Mongo indexes are in place to ensure:
	// 1. fast lookup
	// 2. single document per URL
	mongoHashCollection.ensureIndex(MongoDBObject("hash" -> 1))
	mongoHashCollection.ensureIndex(MongoDBObject("url" -> 1), MongoDBObject("unique" -> true))
	
	// Hash generator
	val hashGenerator = new Hashids("ken.ly.super.secure.salt", 5);
	
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
							val count = 0: java.lang.Integer
							val doc = MongoDBObject()
							doc += "url" -> url
							doc += "hash" -> hash
							doc += "count" -> count
							insertDoc(doc, mongoHashCollection)
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
					val doc = mongoHashCollection.findOne(MongoDBObject("hash" -> hash))
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
			path("[\\w\\d]{5,}".r) { hash => // link processor
				val doc = mongoHashCollection.findOne(MongoDBObject("hash" -> hash))
				doc match {
					case Some(doc) =>
						mongoHashCollection.update(MongoDBObject("hash" -> hash), $inc("count" -> 1))
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