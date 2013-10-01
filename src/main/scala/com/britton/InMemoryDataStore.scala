package com.britton

import scala.concurrent._
import concurrent.Promise
import ExecutionContext.Implicits.global

/**
	In-memory data store.  Contents of this store will be destroyed on JVM exit.
	
	NOTE: This class can only support single-threaded, single-actor usage such as simple testing.
	For all other usage scenarios, MongoDataStore is required.
*/
class MemoryDataStore extends DataStore with Logging {
	
	log.info("Using in-memory data store")
	
 	private val data = collection.mutable.Map[String,Link]()
	
	def saveLink(url:String, hash:String, count:Int) : String = {
		if(!data.contains(hash)) {
			data.put(hash, new Link(url,hash,count))
		}
		hash
	}
	
	def findLink(hash:String) : Option[Link] = {
		val opt = data.get(hash)
		opt match {
			case Some(obj) => Some(obj)
			case None => None
		}
	}
	
	def incrementClicks(hash:String) : Boolean = {
		val current = data(hash)
		val newValue = current.count+1;
		data.update(hash, new Link(current.url, hash, newValue))
		true
	}
	
	def clear() : Boolean = {
		data.clear()
		true
	}
}