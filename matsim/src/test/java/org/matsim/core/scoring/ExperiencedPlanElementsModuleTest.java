package org.matsim.core.scoring;

import com.google.common.eventbus.Subscribe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.ReplayEvents;
import org.matsim.core.events.EventsManagerModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
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
				new ScenarioByInstanceModule(ScenarioUtils.createScenario(config)),
				new ReplayEvents.Module());
		Subscriber subscriber = new Subscriber();
		injector.getInstance(EventsToActivities.class).addActivityHandler(subscriber);
		ReplayEvents replayEvents = injector.getInstance(ReplayEvents.class);
		replayEvents.playEventsFile(matsimTestUtils.getClassInputDirectory() + "events.xml", 0);
		Assert.assertEquals("There are two activities.", 2, subscriber.activityCount);
	}


	private static class Subscriber implements EventsToActivities.ActivityHandler {

		int activityCount = 0;

		@Override
		public void handleActivity(PersonExperiencedActivity activity) {
			activityCount++;
		}

	}

}
