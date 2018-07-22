package org.matsim.core.mobsim.qsim;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Key;
import com.google.inject.name.Names;
import com.vividsolutions.jts.util.Assert;

public class QSimUtilsTest {
	@Test
	public void testOverrides() {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		List<AbstractModule> overrides = Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				bind(Key.get(Integer.class, Names.named("test"))).toInstance(100);
			}
		});

		QSim qsim = QSimUtils.createDefaultQSimWithOverrides(scenario, eventsManager, overrides);

		Assert.equals(100, qsim.getChildInjector().getInstance(Key.get(Integer.class, Names.named("test"))));
	}
}
