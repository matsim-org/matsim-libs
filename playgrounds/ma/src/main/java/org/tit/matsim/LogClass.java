package org.tit.matsim;

import org.apache.log4j.Logger;
import org.junit.Test;
public class LogClass {

    private Logger log = Logger.getLogger(LogClass.class);
    
    @Test
    public void testLog()
    {
    	log.trace("Trace");

    	log.debug("Debug");

    	log.info("Info");

    	log.warn("Warn");

    	log.error("Error");

    	log.fatal("Fatal");
    	
    }
}

