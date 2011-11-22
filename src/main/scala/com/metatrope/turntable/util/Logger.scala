package com.metatrope.turntable.util;

import org.slf4j.LoggerFactory

trait Logger {
    val log = LoggerFactory getLogger(this.getClass)
    
    def debug(s:String) = log.debug(s)
    def info(s:String) = log.info(s)
}
