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
	
	def saveLink(url:String, hash:String, count:Int) : String = {
		val doc = MongoDBObject()
		doc += "url" -> url
		doc += "hash" -> hash
		doc += "count" -> count.asInstanceOf[java.lang.Integer]
		Try(mongoHashCollection.insert(doc))
		hash
	}
	
	def findLink(hash:String) : Option[Link] = {
		val found = mongoHashCollection.findOne(MongoDBObject("hash" -> hash))
		found match {
			case Some(doc) => 
				Some(new Link(doc.get("url").toString, doc.get("hash").toString, doc.get("count").asInstanceOf[Int]))
			case None => None
		}
	}
	
	def incrementClicks(hash:String) : Boolean = {
		mongoHashCollection.findAndModify(MongoDBObject("hash" -> hash), $inc("count" -> 1))
		true
	}
		
	def clear() : Boolean = {
		mongoHashCollection.remove(MongoDBObject())
		true
	}
}