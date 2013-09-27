package com.britton

/**
	A provider & persister of data.  This abstraction allows us to 
	provide different implementations for testing, production, etc.
*/
trait DataStore {
	
	def trackLink(url:String, hash:String, count:Int) : Option[Boolean]
	def findLink(hash:String) : Option[Link]
	def incrementClicks(hash:String) : Option[Boolean]
	def clear() : Option[Boolean]
}