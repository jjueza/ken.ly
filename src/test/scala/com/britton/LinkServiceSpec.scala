package com.britton

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._
import spray.json._
import DefaultJsonProtocol._
import org.specs2.specification._
import com.mongodb.casbah.Imports._

class MyServiceSpec extends Specification with Specs2RouteTest with LinkService {
	
	def actorRefFactory = system

	"LinkService" should {
		
		// Tests for hashing

		"reject hash requests missing required 'url' parameter" in {
			Get("/actions/hash") ~> route ~> check {
				handled must beFalse
			}
		}
		"return a hash in a json string for valid URLs" in {
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
			}
		}
		"return an error message for requests to hash invalid URLs" in {
			Get("/actions/hash?url=1234") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("error")
			}
		}
		
		step(dataStore.clear())
		
		// Tests for statistics
		
		"reject stats requests missing required 'hash' parameter" in {
			Get("/actions/stats") ~> route ~> check {
				handled must beFalse
			}
		}
		"return a valid json document containing stats for a hash" in {
			
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
		
		"increments the clickCount after sending a redirect for the shortened URL" in {
			
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
		
		step(dataStore.clear())
		
		// Test for redirection
		
		"return the correct status code and redirect location from a hash request" in {
			
			//generate a hash
			Get("/actions/hash?url=http://abc.com") ~> route ~> check {
				val json1 = entityAs[String].asJson.convertTo[Map[String,String]]
				json1 must haveKey("hash")
				val hash = json1.get("hash").get

				//click the link!
				Get("/"+hash) ~> route ~> check {
					status === StatusCodes.TemporaryRedirect
					header("Location").getOrElse("").toString === "Location: http://abc.com"
				}
			}
		}
		
		step(dataStore.clear())

		// Un-mapped path testing
		
		"leave GET requests for unknown hashes unhandled" in {
			Get("/kermit") ~> route ~> check {
				handled must beFalse
			}
		}
		
		// Unsupported http methods testing
		
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