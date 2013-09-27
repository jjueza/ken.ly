package com.britton

import org.specs2.mutable.{Specification, After}
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import spray.json._
import DefaultJsonProtocol._
import org.specs2.specification._
import com.mongodb.casbah.Imports._

/**
	Tests for LinkService
*/
class LinkServiceSpec extends Specification with Specs2RouteTest with LinkService with AfterExample {
	sequential
	
	def actorRefFactory = system
	
	//cleanup the databse after each example
	def after = dataStore.clear()
	after match { case _ => None }

	"LinkService hash" should {

		"reject requests missing required 'url' parameter" in {
			Get("/actions/hash") ~> route ~> check {
				handled must beFalse
			}
		}
		
		"return a hash for valid URLs" in {
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
			}
		}
		
		"return an error message for invalid URLs" in {
			Get("/actions/hash?url=1234") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("error")
			}
		}
	}
	
	"LinkService stats" should {
		
		"reject requests missing required 'hash' parameter" in {
			Get("/actions/stats") ~> route ~> check {
				handled must beFalse
			}
		}
		
		"return a clickCount of 0 for un-clicked links" in {
			
			//generate a hash
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
				val hash = json1.get("hash").get
				
				//get the stats
				Get("/actions/stats?hash="+hash) ~> route ~> check {
					val json2 = entityAs[String].asJson.convertTo[Map[String,String]]
					json2 must haveKey("clickCount")
					val count = json2.get("clickCount").getOrElse("")
					count.toInt mustEqual 0
				}
			}
		}
		
		"return a clickCount of N for N-clicked links" in {
			
			//generate a hash
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
				val hash = json1.get("hash").get
				
				//click the link!
				Get("/"+hash) ~> route ~> check {}
				
				//get the stats
				Get("/actions/stats?hash="+hash) ~> route ~> check {
					val json2 = entityAs[String].asJson.convertTo[Map[String,String]]
					json2 must haveKey("clickCount")
					val count = json2.get("clickCount").getOrElse("")
					count.toInt mustEqual 1
				}
			}
		}
		
		"return a 404 for unknown hashes" in {
			Get("/actions/stats?hash=yOuRhAsH") ~> route ~> check {
				status === StatusCodes.NotFound
			}
		}
	}
		
	"LinkService redirect" should {
		
		"return a 301 and a Location header for known hashes" in {
			
			//generate a hash
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
				val hash = json1.get("hash").get

				//click the link!
				Get("/"+hash) ~> route ~> check {
					status === StatusCodes.MovedPermanently
					header("Location").getOrElse("").toString === "Location: http://abc.com"
				}
			}
		}
		
		"return a 404 for unknown hashes" in {
			Get("/yOuRhAsH") ~> route ~> check {
				status === StatusCodes.NotFound
			}
		}
	}

	"LinkService" should {
		
		"leave GET requests for unknown paths unhandled" in {
			Get("/kermit") ~> route ~> check {
				handled must beFalse
			}
		}
		
		"return a MethodNotAllowed error for PUT requests to the root path" in {
			Put() ~> sealRoute(route) ~> check {
				status === MethodNotAllowed
				entityAs[String] === "HTTP method not allowed, supported methods: GET"
			}
		}
		
		"return a MethodNotAllowed error for POST requests to the root path" in {
			Post() ~> sealRoute(route) ~> check {
				status === MethodNotAllowed
				entityAs[String] === "HTTP method not allowed, supported methods: GET"
			}
		}
		
		"return a MethodNotAllowed error for DELETE requests to the root path" in {
			Delete() ~> sealRoute(route) ~> check {
				status === MethodNotAllowed
				entityAs[String] === "HTTP method not allowed, supported methods: GET"
			}
		}
	}
}