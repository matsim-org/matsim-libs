package org.matsim.core.scoring;

import com.google.common.eventbus.Subscribe;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionModule;
import org.matsim.testcases.MatsimTestUtils;

public class ExperiencedPlanElementsModuleTest {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testExperiencedPlanElementsModule() {
		Config config = ConfigUtils.createConfig();
		com.google.inject.Injector injector = Injector.createInjector(config,
				new ExperiencedPlanElementsModule(),
				new EventsManagerModule(),
				new CharyparNagelScoringFunctionModule(),
				new ScenarioByInstanceModule(ScenarioUtils.createScenario(config)),
				new ReplayEvents.Module());
		injector.getInstance(ExperiencedPlanElementsService.class).register(new ThrowingSubscriber());
		ReplayEvents instance = injector.getInstance(ReplayEvents.class);
		instance.playEventsFile(matsimTestUtils.getClassInputDirectory() + "/events.xml");
	}


	private static class ThrowingSubscriber {
		@Subscribe
		public void throwException(PersonExperiencedActivity activity) {
			System.out.println(activity);
			throw new RuntimeException("Wurst");
		}
	}

}
