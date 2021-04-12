package org.matsim.contrib.discrete_mode_choice.examples;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceConfigurator;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.pt.config.TransitConfigGroup.TransitRoutingAlgorithmType;

public class TestSiouxFalls {
	@Test
	public void testSiouxFallsWithSubtourModeChoiceReplacement() {
		URL scenarioURL = ExamplesUtils.getTestScenarioURL("siouxfalls-2014");

		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(scenarioURL, "config_default.xml"));
		config.transit().setRoutingAlgorithmType(TransitRoutingAlgorithmType.DijkstraBased);
		DiscreteModeChoiceConfigurator.configureAsSubtourModeChoiceReplacement(config);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(1);

		config.qsim().setFlowCapFactor(10000.0);
		config.qsim().setStorageCapFactor(10000.0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controller = new Controler(scenario);
		controller.addOverridingModule(new DiscreteModeChoiceModule());

		ModeListener listener = new ModeListener();
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(listener);
			}
		});

		controller.run();

		assertEquals(42520, (int) listener.counts.get("pt"));
		assertEquals(132100, (int) listener.counts.get("car"));
		assertEquals(79106, (int) listener.counts.get("walk"));
	}

	static class ModeListener implements PersonArrivalEventHandler {
		private final Map<String, Integer> counts = new HashMap<>();

		@Override
		public void reset(int iteration) {
			counts.clear();
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			String mode = event.getLegMode();

			if (!counts.containsKey(mode)) {
				counts.put(mode, 0);
			}

			counts.put(mode, counts.get(mode) + 1);
		}
	}
}
