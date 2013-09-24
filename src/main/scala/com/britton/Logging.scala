package com.britton

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
	val log = LoggerFactory.getLogger(getClass)
}