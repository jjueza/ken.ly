package com.britton

import com.mongodb.casbah.Imports._
import com.mongodb.MongoException
import scala.util._

/**
	MongoDB data store.  Contents of this data-store can last as long as you'd like.
*/
class MongoDataStore(uri:String) extends DataStore {
	
	println("Using mongo instance: "+uri)
	
	// get a reusable DBCollection from Mongo
	private val mongoURI = MongoClientURI(uri)
	private val mongoHashCollection =  MongoClient(mongoURI)(mongoURI.database.getOrElse("links"))("hashes")
	
	// ensure Mongo indexes are in place for:
	// 1. fast lookup
	// 2. single document per URL
	mongoHashCollection.ensureIndex(MongoDBObject("hash" -> 1))
	mongoHashCollection.ensureIndex(MongoDBObject("url" -> 1), MongoDBObject("unique" -> true))
	
	def trackLink(url:String, hash:String, count:Int) = {
		val doc = MongoDBObject()
		doc += "url" -> url
		doc += "hash" -> hash
		doc += "count" -> count.asInstanceOf[java.lang.Integer]
		Try(mongoHashCollection.insert(doc))
	}
	
	def findLink(hash:String) : Option[Link] = {
		val found = mongoHashCollection.findOne(MongoDBObject("hash" -> hash))
		found match {
			case Some(doc) => Option(new Link(doc.get("url").toString, doc.get("hash").toString, doc.get("count").asInstanceOf[Int]))
			case None => None
		}
	}
	
	def incrementClicks(link:Link) =
		mongoHashCollection.update(MongoDBObject("hash" -> link.hash), $inc("count" -> 1))
		
	def clear() = mongoHashCollection.remove(MongoDBObject())
}