package com.britton

import org.slf4j.{Logger, LoggerFactory}

/**
	Reusable trait for logging with SLF4J
*/
trait Logging {
	val log = LoggerFactory.getLogger(getClass)
}