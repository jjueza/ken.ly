package com.britton

/**
	In-memory data store.  Contents of this store will be destroyed on JVM exit.
	
	NOTE: This class can only support single-threaded, single-actor usage such as simple testing.
	For all other usage scenarios, MongoDataStore is required.
*/
class MemoryDataStore extends DataStore with Logging {
	
	log.info("Using in-memory data store")
	
 	private val data = collection.mutable.Map[String,Link]()
	
	def trackLink(url:String, hash:String, count:Int) : Option[Boolean] = {
		if(!data.contains(hash)) {
			data.put(hash, new Link(url,hash,count))
		}
		Option(true)
	}
	
	def findLink(hash:String) : Option[Link] = data.get(hash)
	
	def incrementClicks(hash:String) : Option[Boolean] = {
		val current = data(hash)
		val newValue = current.count+1;
		data.update(hash, new Link(current.url, hash, newValue))
		Option(true)
	}
	
	def clear() : Option[Boolean] = {
		data.clear()
		Option(true)
	}
}