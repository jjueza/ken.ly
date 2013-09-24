package com.britton

/**
	In-memory data store.  Contents of this store will be destroyed on JVM exit.
*/
class MemoryDataStore extends DataStore with Logging {
	
	log.info("Using in-memory data store")
	
 	private val data = collection.mutable.Map[String,Link]()
	
	def trackLink(url:String, hash:String, count:Int) = {
		if(!data.contains(hash)) {
			data.put(hash, new Link(url,hash,count))
		}
	}
	
	def findLink(hash:String) : Option[Link] = data.get(hash)
	
	def incrementClicks(hash:String) = {
		val current = data(hash)
		data.update(hash, new Link(current.url, hash, current.count+1))
	}
	
	def clear() = data.clear()
}