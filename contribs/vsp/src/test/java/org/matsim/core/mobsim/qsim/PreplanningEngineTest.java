package org.matsim.core.mobsim.qsim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Singleton;

class PreplanningEngineTest{
	// Cannot use otfvis if in core matsim.  --> In vsp contrib for time being.  kai, apr'24
	private static final Logger log = LogManager.getLogger(PreplanningEngineTest.class );

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled
	@Test public void test() {
		// I am interested here in testing this NOT with DRT but with other modes.  kai, apr'24
		// In the somewhat longer run, should work together with fleetpy (of TUM).  kai, apr'24

		Config config = ConfigUtils.loadConfig( IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ) );

		config.controller().setOutputDirectory( utils.getOutputDirectory() );
		config.controller().setLastIteration( 0 );

		QSimComponentsConfigGroup componentsConfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );

		componentsConfig.removeActiveComponent( ActivityEngineModule.COMPONENT_NAME );
		componentsConfig.addActiveComponent( ActivityEngineWithWakeup.COMPONENT_NAME );

		componentsConfig.addActiveComponent( PreplanningEngineQSimModule.COMPONENT_NAME );

		Scenario scenario = ScenarioUtils.loadScenario( config );
		for( Person person : scenario.getPopulation().getPersons().values() ){
			PreplanningUtils.setPrebookingOffset_s( person.getSelectedPlan(), 900. );
		}

		Controler controler = new Controler( scenario );

		controler.addOverridingQSimModule( new AbstractQSimModule(){
			@Override protected void configureQSim(){
				bind(PreplanningEngine.class).asEagerSingleton();
				addQSimComponentBinding(PreplanningEngineQSimModule.COMPONENT_NAME).to(PreplanningEngine.class).in( Singleton.class );
				// Does the following:
				// * on prepare sim: register all departure handlers that implement the TripInfo.Provider interface
				// *

				// the above just installs the functionality; it also needs to be requested (from config).

				// needs to go along with ActivityEngineWithWakeup.  complains if not bound.
				// yy is, however, ok if bound but not activated.  --?? --> should end up in same module!

				addQSimComponentBinding( ActivityEngineWithWakeup.COMPONENT_NAME ).to( ActivityEngineWithWakeup.class ).in( Singleton.class );

			}
		} );

		if ("true".equals(System.getProperty("runOTFVis"))) {
			// This will start otfvis if property is set
			controler.addOverridingModule(new OTFVisLiveModule() );
			// !! does not work together with parameterized tests :-( !!
		}

		controler.run();
	}

}
