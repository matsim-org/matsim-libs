package org.matsim.contrib.minibus;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.performance.raptor.Raptor;
import org.matsim.contrib.minibus.performance.raptor.RaptorDisutility;
import org.matsim.contrib.minibus.performance.raptor.TransitRouterQuadTree;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class RaptorTest {
	
	
	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();
	
	@Test
	public void test() {
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
	
	
	private static class RaptorTransitRouterProvider implements Provider<TransitRouter> {
		@Inject private Config config;
		@Inject private TransitSchedule schedule;

		@Override
		public TransitRouter get() {
			TransitRouterConfig transitRouterConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
			RaptorDisutility raptorDisutility = new RaptorDisutility(transitRouterConfig, 0., 0.);
			TransitRouterQuadTree transitRouterQuadTree = new TransitRouterQuadTree(raptorDisutility);
			transitRouterQuadTree.initializeFromSchedule(schedule, transitRouterConfig.getBeelineWalkConnectionDistance());
			return new Raptor(transitRouterQuadTree, raptorDisutility, transitRouterConfig);
		}
	}
}
