package org.matsim.codeexamples.programming.scenario;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ScenarioElementAndInjection{
	private static final Logger log = LogManager.getLogger( ScenarioElementAndInjection.class ) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * This is testing at what phase a scenario element would be available.
	 */
	@Test
	public void test1() {
		// not really a test in its current form!

		Config config = ConfigUtils.createConfig() ;
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.controler().setLastIteration( 2 );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		RoadPricingSchemeImpl scheme = RoadPricingUtils.addOrGetMutableRoadPricingScheme( scenario );
		RoadPricingUtils.setType( scheme, RoadPricingScheme.TOLL_TYPE_LINK );

		Controler controler = new Controler( scenario ) ;

		controler.addOverridingModule( new AbstractModule(){
			@Override public void install(){
				bind( MyClass.class );
				this.addControlerListenerBinding().toInstance( new StartupListener(){
					@Inject MyClass abc ;
					@Override public void notifyStartup( StartupEvent event ){
						abc.abc();
					}
				} );
			}
		} ) ;

		controler.run() ;

	}

	static class MyClass{
		@Inject MyClass( Scenario scenario ){
			RoadPricingScheme scheme = (RoadPricingScheme) scenario.getScenarioElement( RoadPricingScheme.ELEMENT_NAME );
			Gbl.assertNotNull(scheme);
			log.warn("roadPricingType="+scheme.getType());
			Gbl.assertNotNull( scheme.getType() );
		}
		void abc() {
			log.warn("here") ;
		}
	}
}
