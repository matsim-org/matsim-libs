package org.matsim.codeexamples.mobsim.replaceAgentFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.router.TripRouter;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Inject;

public class RunReplaceAgentFactoryExampleTest {
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void testMain() {
		try {
			final RunReplaceAgentFactoryExample main = new RunReplaceAgentFactoryExample();;
			final Config config = main.prepareConfig();
			config.controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controller().setLastIteration( 2 );
			config.controller().setOutputDirectory( utils.getOutputDirectory() );
			
			Controler controler = main.prepareControler() ;
			
			final EventsCounter eventsCounter = new EventsCounter();

			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( eventsCounter ) ;
				}
			} );
			
			main.run() ;
			
			Assertions.assertEquals( 9024, eventsCounter.getCnt() );
			
		} catch (Exception ee) {
			ee.printStackTrace();
			Assertions.fail() ;
		}
	}
	
	private static class EventsCounter implements BasicEventHandler {
		private long cnt = 0 ;
		@Inject Scenario scenario ;
		@Inject TripRouter tripRouter ; // injecting TripRouter at level of controler is ok
		@Override public void handleEvent( final Event event ) {
			cnt++ ;
		}
		long getCnt() {
			return cnt;
		}
	}
}
