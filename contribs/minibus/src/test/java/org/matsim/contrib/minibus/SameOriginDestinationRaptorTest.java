package org.matsim.contrib.minibus;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.performance.raptor.RaptorTransitRouterProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.testcases.MatsimTestUtils;

public class SameOriginDestinationRaptorTest {
	
	
	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();

	/**
	 * Currently (Jan'18) raptor throws an exception if from and to transit stops are same.
	 */
	@Test
	public void sameFromAndToTransitStopTest() {
		String config = "test/input/org/matsim/contrib/minibus/example-scenario/raptorFixMinimalExample/config_raptor.xml";
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(config));
		scenario.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(RaptorTransitRouterProvider.class);
			}
		});
		controler.run();
	}
}
