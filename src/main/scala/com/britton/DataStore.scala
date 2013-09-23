package com.britton

/**
	A provider & persister of data.  This abstraction allows us to 
	provide different implementations for testing, production, etc.
*/
abstract class DataStore {
	
	def trackLink(url:String, hash:String, count:Int)
	def findLink(hash:String) : Option[Link]
	def incrementClicks(link:Link)
	def clear()
}