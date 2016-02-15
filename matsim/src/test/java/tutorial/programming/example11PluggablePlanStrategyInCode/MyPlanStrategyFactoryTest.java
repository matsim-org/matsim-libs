package tutorial.programming.example11PluggablePlanStrategyInCode;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.scenario.ScenarioUtils;



public class MyPlanStrategyFactoryTest {

	@Test
	public final void testGet()
	{
		//set up
		EventsManager manager = EventsUtils.createEventsManager();
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		MyPlanStrategyFactory strategyFactory = new MyPlanStrategyFactory(manager, scenario);
		
		//act
		PlanStrategy strategy = strategyFactory.get();
		
		//assert
		Assert.assertNotNull("strategy was null", strategy);
	}
}
