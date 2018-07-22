package org.matsim.core.mobsim.qsim.components;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class QSimComponentsTest {
	@Test
	public void testManualConfiguration() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngine mockEngine = new MockEngine();

		Collection<AbstractQSimPlugin> plugins = Collections.singleton(new AbstractQSimPlugin(config) {
			public Collection<? extends Module> modules() {
				return Collections.singleton(new AbstractModule() {
					@Override
					protected void configure() {
						bind(MockEngine.class).toInstance(mockEngine);
					}
				});
			}

			public Map<String, Class<? extends MobsimEngine>> engines() {
				return Collections.singletonMap("MockEngine", MockEngine.class);
			}
		});

		List<org.matsim.core.controler.AbstractModule> modules = Collections
				.singletonList(new org.matsim.core.controler.AbstractModule(config) {
					@Override
					public void install() {
						QSimComponents components = new QSimComponents();
						components.activeMobsimEngines.add("MockEngine");
						bind(QSimComponents.class).toInstance(components);
					}
				});

		QSim qsim = QSimUtils.createQSim(scenario, eventsManager, modules, plugins);
		qsim.run();

		Assert.assertTrue(mockEngine.isCalled);
	}

	@Test
	public void testUseConfigGroup() {
		Config config = ConfigUtils.createConfig();

		QSimComponentsConfigGroup componentsConfig = new QSimComponentsConfigGroup();
		config.addModule(componentsConfig);

		componentsConfig.setActiveMobsimEngines(Collections.singletonList("MockEngine"));
		componentsConfig.setActiveActivityHandlers(Collections.emptyList());
		componentsConfig.setActiveDepartureHandlers(Collections.emptyList());
		componentsConfig.setActiveAgentSources(Collections.emptyList());

		Scenario scenario = ScenarioUtils.createScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		MockEngine mockEngine = new MockEngine();

		Collection<AbstractQSimPlugin> plugins = Collections.singleton(new AbstractQSimPlugin(config) {
			public Collection<? extends Module> modules() {
				return Collections.singleton(new AbstractModule() {
					@Override
					protected void configure() {
						bind(MockEngine.class).toInstance(mockEngine);
					}
				});
			}

			public Map<String, Class<? extends MobsimEngine>> engines() {
				return Collections.singletonMap("MockEngine", MockEngine.class);
			}
		});

		QSim qsim = QSimUtils.createQSim(scenario, eventsManager, plugins);
		qsim.run();

		Assert.assertTrue(mockEngine.isCalled);
	}

	private class MockEngine implements MobsimEngine {
		public boolean isCalled = false;

		@Override
		public void doSimStep(double time) {
			isCalled = true;
		}

		@Override
		public void onPrepareSim() {

		}

		@Override
		public void afterSim() {

		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {

		}
	}
}
