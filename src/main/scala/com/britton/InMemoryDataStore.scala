package com.britton

import scala.concurrent._
import concurrent.Promise

/**
	In-memory data store.  Contents of this store will be destroyed on JVM exit.
	
	NOTE: This class can only support single-threaded, single-actor usage such as simple testing.
	For all other usage scenarios, MongoDataStore is required.
*/
class MemoryDataStore extends DataStore with Logging {
	
	log.info("Using in-memory data store")
	
 	private val data = collection.mutable.Map[String,Link]()
	
	def trackLink(url:String, hash:String, count:Int) : Future[Boolean] = {
		if(!data.contains(hash)) {
			data.put(hash, new Link(url,hash,count))
		}
		Promise.successful(true).future
	}
	
	def findLink(hash:String) : Future[Link] = {
		val opt = data.get(hash)
		opt match {
			case Some(obj) => Promise.successful(obj).future
			case None => Promise.failed(new MissingObjectException()).future
		}
	}
	
	def incrementClicks(hash:String) : Future[Boolean] = {
		val current = data(hash)
		val newValue = current.count+1;
		data.update(hash, new Link(current.url, hash, newValue))
		Promise.successful(true).future
	}
	
	def clear() : Future[Boolean] = {
		data.clear()
		Promise.successful(true).future
	}
}