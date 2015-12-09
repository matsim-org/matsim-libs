package org.matsim.core.controler;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class InjectionBeforeControlerTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testInjectionBeforeControler() {
		Config config = testUtils.loadConfig("examples/equil/config.xml");
		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		final Scenario scenario = ScenarioUtils.createScenario(config);
		final EventsManager eventsManager = EventsUtils.createEventsManager();
		Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new ControlerDefaultsModule());
				install(new ControlerDefaultCoreListenersModule());
				bind(Scenario.class).toInstance(scenario);
				bind(EventsManager.class).toInstance(eventsManager);
				bind(OutputDirectoryHierarchy.class).asEagerSingleton();
				bind(IterationStopWatch.class).asEagerSingleton();
				bind(ControlerI.class).to(Controler.class).asEagerSingleton();
			}
		});
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();
	}

}
