package tutorial.programming.eventsHandler;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.matsim.core.utils.io.IOUtils;

import tutorial.programming.example06EventsHandling.RunEventsHandlingWithControlerExample;

public class IntegrationTest {
    
    @Test
	public final void testMain() {
	    try {
		    IOUtils.deleteDirectory( new File( RunEventsHandlingWithControlerExample.outputDirectory ) );
	    } catch ( Exception ee ) {
		    // deletion may fail; is ok.
	    }
		try {
			RunEventsHandlingWithControlerExample.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running eventHandler example: "+ee ) ;
		}
		IOUtils.deleteDirectory( new File( RunEventsHandlingWithControlerExample.outputDirectory ) );
		// if this fails, then it is a test failure (since the directory should have been constructed)
	}

}
