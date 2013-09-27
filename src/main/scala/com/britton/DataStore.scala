package com.britton

import scala.concurrent._

/**
	A provider & persister of data.  This abstraction allows us to 
	provide different implementations for testing, production, etc.
*/
trait DataStore {
	
	def trackLink(url:String, hash:String, count:Int) : Future[Boolean]
	def findLink(hash:String) : Future[Link]
	def incrementClicks(hash:String) : Future[Boolean]
	def clear() : Future[Boolean]
}

class MissingObjectException extends Throwable {}