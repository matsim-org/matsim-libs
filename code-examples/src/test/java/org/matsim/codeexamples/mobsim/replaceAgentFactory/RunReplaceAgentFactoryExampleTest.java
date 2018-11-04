package org.matsim.codeexamples.mobsim.replaceAgentFactory;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.router.TripRouter;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Inject;

public class RunReplaceAgentFactoryExampleTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test public void testMain() {
		try {
			final RunReplaceAgentFactoryExample main = new RunReplaceAgentFactoryExample();;
			final Config config = main.prepareConfig();
			config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
			config.controler().setLastIteration( 2 );
			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			
			Controler controler = main.prepareControler() ;
			
			final EventsCounter eventsCounter = new EventsCounter();

			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
					this.addEventHandlerBinding().toInstance( eventsCounter ) ;
				}
			} );
			
			main.run() ;
			
			Assert.assertEquals( 9024, eventsCounter.getCnt() );
			
		} catch (Exception ee) {
			ee.printStackTrace();
			Assert.fail() ;
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
