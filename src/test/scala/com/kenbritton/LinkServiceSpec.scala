package com.kenbritton

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

class MyServiceSpec extends Specification with Specs2RouteTest with LinkService {
	
  def actorRefFactory = system
  
  "LinkService" should {

    "reference the hash parameter in the resulting JSON" in {
      Get("/actions/stats?hash=abc") ~> route ~> check {
        entityAs[String] must contain("ken.ly/abc")
      }
    }

    "reference the url parameter in the resulting JSON" in {
      Get("/actions/hash?url=abc.com") ~> route ~> check {
        entityAs[String] must contain("abc.com")
      }
    }

    "leave GET requests to other paths unhandled" in {
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
  }
}