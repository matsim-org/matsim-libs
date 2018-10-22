package org.matsim.core.mobsim.qsim;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class AbstractQSimModuleTest {	
	@Test
	public void testOverrides() {
		AbstractQSimModule moduleA = new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				getConfig().createModule("testA");
				bind(String.class).toInstance("testAString");
			}
		};

		AbstractQSimModule moduleB = new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				getConfig().createModule("testB");
				bind(String.class).toInstance("testBString");
			}
		};

		AbstractQSimModule composite = AbstractQSimModule.overrideQSimModules(Collections.singleton(moduleA),
				Collections.singletonList(moduleB));

		Config config = ConfigUtils.createConfig();
		composite.setConfig(config);

		Injector injector = Guice.createInjector(composite);

		Assert.assertTrue(config.getModules().containsKey("testA"));
		Assert.assertTrue(config.getModules().containsKey("testB"));

		Assert.assertEquals("testBString", injector.getInstance(String.class));
	}

	@Test
	public void testOverrideAgentFactory() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
		person.addPlan(scenario.getPopulation().getFactory().createPlan());
		scenario.getPopulation().addPerson(person);

		AtomicLong value = new AtomicLong(0);

		Controler controler = new Controler(scenario);
		controler.addOverridingQSimModule(new TestQSimModule(value));
		controler.run();

		Assert.assertTrue(value.get() > 0);
	}

	private class TestQSimModule extends AbstractQSimModule {
		private final AtomicLong value;

		public TestQSimModule(AtomicLong value) {
			this.value = value;
		}

		@Override
		protected void configureQSim() {
			bind(AtomicLong.class).toInstance(value);
			bind(AgentFactory.class).to(TestAgentFactory.class).asEagerSingleton();
		}
	}

	static private class TestAgentFactory implements AgentFactory {
		private final AgentFactory delegate;
		private final AtomicLong value;

		@Inject
		public TestAgentFactory(Netsim simulation, AtomicLong value) {
			delegate = new DefaultAgentFactory(simulation);
			this.value = value;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			value.incrementAndGet();
			return delegate.createMobsimAgentFromPerson(p);
		}
	}

	@Test
	public void testAddEngine() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		TestEngine engine = new TestEngine();

		Controler controler = new Controler(scenario);

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bindNamedComponent(TestEngine.class, "MyEngine").toInstance(engine);
			}
		});

		controler.configureQSimComponents(components -> {
			components.addNamedComponent("MyEngine");
		});

		controler.run();

		Assert.assertTrue(engine.called);
	}

	static private class TestEngine implements MobsimEngine {
		public boolean called = false;

		@Override
		public void doSimStep(double time) {
			called = true;
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
