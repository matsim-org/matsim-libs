package org.matsim.codeexamples.strategies.pluggablePlanStrategyInCode;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.codeexamples.strategies.pluggablePlanStrategyInCode.MyPlanStrategyProvider;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.scenario.ScenarioUtils;


public class MyPlanStrategyProviderTest {

	@Test
	public final void testGet()
	{
		//set up
		EventsManager manager = EventsUtils.createEventsManager();
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MyPlanStrategyProvider strategyFactory = new MyPlanStrategyProvider(manager, scenario);
		
		//act
		PlanStrategy strategy = strategyFactory.get();
		
		//assert
		Assert.assertNotNull("strategy was null", strategy);
	}
}
