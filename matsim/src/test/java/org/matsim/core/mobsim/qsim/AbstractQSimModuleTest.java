
/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractQSimModuleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

 package org.matsim.core.mobsim.qsim;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
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
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

	public class AbstractQSimModuleTest {
	 @Test
	 void testOverrides() {
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

		Assertions.assertTrue(config.getModules().containsKey("testA"));
		Assertions.assertTrue(config.getModules().containsKey("testB"));

		Assertions.assertEquals("testBString", injector.getInstance(String.class));
	}

	 @Test
	 void testOverrideAgentFactory() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Person person = populationFactory.createPerson(Id.createPersonId("person"));
		Plan plan =  populationFactory.createPlan();
		plan.addActivity( populationFactory.createActivityFromLinkId("type", Id.createLinkId("0")));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		AtomicLong value = new AtomicLong(0);

		Controler controler = new Controler(scenario);
		controler.addOverridingQSimModule(new TestQSimModule(value));
		controler.run();

		Assertions.assertTrue(value.get() > 0);
	}

	 @Test
	 void testOverrideAgentFactoryTwice() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		Person person = populationFactory.createPerson(Id.createPersonId("person"));
		Plan plan =  populationFactory.createPlan();
		plan.addActivity( populationFactory.createActivityFromLinkId("type", Id.createLinkId("0")));
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		AtomicLong value1 = new AtomicLong(0);
		AtomicLong value2 = new AtomicLong(0);

		Controler controler = new Controler(scenario);
		controler.addOverridingQSimModule(new TestQSimModule(value1));
		controler.addOverridingQSimModule(new TestQSimModule(value2));
		controler.run();

		Assertions.assertTrue(value1.get() == 0);
		Assertions.assertTrue(value2.get() > 0);
	}

	private static class TestQSimModule extends AbstractQSimModule {
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
		public TestAgentFactory(Netsim simulation, AtomicLong value, TimeInterpretation timeInterpretation) {
			delegate = new DefaultAgentFactory(simulation, timeInterpretation);
			this.value = value;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			value.incrementAndGet();
			return delegate.createMobsimAgentFromPerson(p);
		}
	}

	 @Test
	 void testAddEngine() {
		Config config = ConfigUtils.createConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(0);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		TestEngine engine = new TestEngine();

		Controler controler = new Controler(scenario);

		controler.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addQSimComponentBinding("MyEngine").toInstance(engine);
			}
		});

		controler.configureQSimComponents(components -> {
			components.addNamedComponent("MyEngine");
		});

		controler.run();

		Assertions.assertTrue(engine.called);
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
