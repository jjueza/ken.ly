package com.britton

import com.mongodb.casbah.Imports._
import com.mongodb.MongoException
import scala.util._
import scala.concurrent._

/**
	MongoDB data store.  Contents of this data-store can last as long as you'd like ;)
*/
class MongoDataStore(uri:String) extends DataStore with Logging {
	
	log.info("Using mongo instance: "+uri)
	
	// get a reusable DBCollection from Mongo & ensure indexes are in place
	private val mongoURI = MongoClientURI(uri)
	private val mongoHashCollection =  MongoClient(mongoURI)(mongoURI.database.getOrElse("links"))("hashes")
	mongoHashCollection.setWriteConcern(WriteConcern.Safe)
	mongoHashCollection.ensureIndex(MongoDBObject("hash" -> 1))
	mongoHashCollection.ensureIndex(MongoDBObject("url" -> 1), MongoDBObject("unique" -> true))
	
	def trackLink(url:String, hash:String, count:Int) : Future[Boolean] = {
		val doc = MongoDBObject()
		doc += "url" -> url
		doc += "hash" -> hash
		doc += "count" -> count.asInstanceOf[java.lang.Integer]
		Try(mongoHashCollection.insert(doc))
		Promise.successful(true).future
	}
	
	def findLink(hash:String) : Future[Link] = {
		val found = mongoHashCollection.findOne(MongoDBObject("hash" -> hash))
		found match {
			case Some(doc) => 
				Promise.successful(new Link(doc.get("url").toString, doc.get("hash").toString, doc.get("count").asInstanceOf[Int])).future
			case None =>
				Promise.failed(new MissingObjectException()).future
		}
	}
	
	def incrementClicks(hash:String) : Future[Boolean] = {
		mongoHashCollection.findAndModify(MongoDBObject("hash" -> hash), $inc("count" -> 1))
		Promise.successful(true).future
	}
		
	def clear() : Future[Boolean] = {
		mongoHashCollection.remove(MongoDBObject())
		Promise.successful(true).future
	}
}