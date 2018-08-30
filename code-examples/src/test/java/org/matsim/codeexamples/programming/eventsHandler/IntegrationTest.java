package org.matsim.codeexamples.programming.eventsHandler;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.matsim.codeexamples.events.eventsHandling.RunEventsHandlingWithControlerExample;
import org.matsim.core.utils.io.IOUtils;

public class IntegrationTest {
    
    @Test
	public final void testMain() {
	    try {
			IOUtils.deleteDirectoryRecursively(new File( RunEventsHandlingWithControlerExample.outputDirectory ).toPath());
		} catch ( Exception ee ) {
		    // deletion may fail; is ok.
	    }
		try {
			RunEventsHandlingWithControlerExample.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running eventHandler example: "+ee ) ;
		}
		IOUtils.deleteDirectoryRecursively(new File( RunEventsHandlingWithControlerExample.outputDirectory ).toPath());
		// if this fails, then it is a test failure (since the directory should have been constructed)
	}

}
