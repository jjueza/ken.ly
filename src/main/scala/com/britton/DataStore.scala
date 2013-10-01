package com.britton

import scala.concurrent._

/**
	A provider & persister of data.  This abstraction allows us to 
	provide different implementations for testing, production, etc.
*/
trait DataStore {
	
	def saveLink(url:String, hash:String, count:Int) : String
	def findLink(hash:String) : Option[Link]
	def incrementClicks(hash:String) : Boolean
	def clear() : Boolean
}

class MissingObjectException extends Throwable {}