package tutorial.programming.eventsHandler;

import static org.junit.Assert.fail;

import org.junit.Test;

import tutorial.programming.example06EventsHandling.RunEventsHandlingWithControlerExample;

public class IntegrationTest {
    
    @Test
	public final void testMain() {
		try {
			RunEventsHandlingWithControlerExample.main(null);
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running eventHandler example: "+ee ) ;
		}
	}

}
