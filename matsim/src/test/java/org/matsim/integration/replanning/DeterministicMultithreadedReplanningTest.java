/* *********************************************************************** *
 * project: org.matsim.*
 * DeterministicMultithreadedReplanningTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.integration.replanning;

import com.google.inject.Singleton;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Provider;

/**
 * Tests that the re-planning process of MATSim generates the same results every time, even when
 * multiple threads are used.  This implies that all re-planning modules that use random numbers
 * make use of {@link MatsimRandom#getLocalInstance()}, and do not use the global
 * {@link MatsimRandom#random}.
 *
 * The tests are usually done by running a simulation twice for a few iterations, with the number
 * of possible threads set to a value larger than 1 (e.g. 4). This forces that several threads
 * are used for re-planning, making it very unlikely that the outcome after several iterations
 * is byte-identical if the modules would use the global {@link MatsimRandom#random}.
 *
 * @author mrieser
 */
public class DeterministicMultithreadedReplanningTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Tests that the {@link TimeAllocationMutator} generates always the same results
	 * with the same number of threads.
	 */
	@Test
	public void testTimeAllocationMutator() {
		int lastIteration = 5;
		Config config = testUtils.loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1


		{
			StrategyManager strategyManager = new StrategyManager();
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run1/");
			TestControler controler = new TestControler(config, strategyManager);
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new TimeAllocationMutator(TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario()), config.plans(), config.timeAllocationMutator(), config.global()));
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);
			controler.run();
		}

		{
			StrategyManager strategyManager = new StrategyManager();
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run2/");
			TestControler controler = new TestControler(config, strategyManager);
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new TimeAllocationMutator(TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario()), config.plans(), config.timeAllocationMutator(), config.global()));
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);
			controler.run();
		}

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			Assert.assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			Assert.assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}

	/**
	 * Tests that the combination of {@link ReRoute} and {@link TimeAllocationMutator} always generates 
	 * the same results with the same number of threads.
	 */
	@Test
	public void testReRouteTimeAllocationMutator() {
		int lastIteration = 5;
		Config config = testUtils.loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1

		{
			// setup run1
			StrategyManager strategyManager = new StrategyManager();
			strategyManager.setMaxPlansPerAgent(5);
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run1/");
			TestControler controler = new TestControler(config, strategyManager);
			strategy.addStrategyModule(new ReRoute(controler.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario()))); // finish strategy configuration
			strategy.addStrategyModule(new TimeAllocationMutator(TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario()), config.plans(), config.timeAllocationMutator(), config.global()));
			controler.run();
		}
		{
			// setup run2
			StrategyManager strategyManager2 = new StrategyManager();
			strategyManager2.setMaxPlansPerAgent(5);
			PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
			strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run2/");
			TestControler controler2 = new TestControler(config, strategyManager2);
			strategy2.addStrategyModule(new ReRoute(controler2.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler2.getScenario()))); // finish strategy configuration
			strategy2.addStrategyModule(new TimeAllocationMutator(TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler2.getScenario()), config.plans(), config.timeAllocationMutator(), config.global()));
			controler2.run();
		}

		for (int i = 0; i <= lastIteration; i++) {
			
			long cksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			
			Assert.assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}
		
		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			Assert.assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}
	
	/**
	 * Tests that the generic {@link ReRoute} generates always the same results
	 * REGARDLESS the number of threads using only one agent.
	 */
	@Test
	public void testReRouteOneAgent() {
		int lastIteration = 5;

		Config config = testUtils.loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1
		config.plans().setInputFile(testUtils.getClassInputDirectory() + "plans1.xml");
		{
			// setup run1
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			StrategyManager strategyManager = new StrategyManager();
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run1/");
			TestControler controler = new TestControler(config, strategyManager);
			strategy.addStrategyModule(new ReRoute(controler.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario())));
			controler.run();
		}
		{
			config.global().setNumberOfThreads(3); // use a different number of threads because the result must be the same
			// setup run2
			PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
			StrategyManager strategyManager2 = new StrategyManager();
			strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run2/");
			TestControler controler2 = new TestControler(config, strategyManager2);
			strategy2.addStrategyModule(new ReRoute(controler2.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler2.getScenario())));

			controler2.run();
		}

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			Assert.assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			Assert.assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}

	}

	/**
	 * Tests that the generic {@link ReRoute} generates always the same results
	 * REGARDLESS the same number of threads.
	 */
	@Test
	public void testReRoute() {
		int lastIteration = 5;
		Config config = testUtils.loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1

		{
			// setup run1
			PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
			StrategyManager strategyManager = new StrategyManager();
			strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run1/");
			TestControler controler = new TestControler(config, strategyManager);
			strategy.addStrategyModule(new ReRoute(controler.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler.getScenario())));
			controler.run();
		}

		{
			config.global().setNumberOfThreads(3); // use a different number of threads because the result must be the same
			// setup run2
			PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
			StrategyManager strategyManager2 = new StrategyManager();
			strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);

			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "/run2/");
			TestControler controler2 = new TestControler(config, strategyManager2);
			strategy2.addStrategyModule(new ReRoute(controler2.getScenario(), TripRouterFactoryBuilderWithDefaults.createDefaultTripRouterFactoryImpl(controler2.getScenario())));
			controler2.run();
		}

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			Assert.assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			Assert.assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}

	/**
	 * A simple Controler for the tests above to overwrite the StrategyManager.
	 *
	 * @author mrieser
	 */
	private static class TestControler {
		Controler controler ;

		private StrategyManager manager;

		public TestControler(final Config config, final StrategyManager manager) {
			this( ScenarioUtils.loadScenario( config ) , manager );
		}

		public TestControler(final Scenario scenario, final StrategyManager manager) {
			controler = new Controler( scenario ) ;
			controler.getConfig().controler().setCreateGraphs(false);
			controler.getConfig().controler().setWriteEventsInterval(1);
			controler.getConfig().controler().setDumpDataAtEnd(false);
			this.manager = manager ;
			controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
					bind(StrategyManager.class).toProvider(new com.google.inject.Provider<StrategyManager>() {
                        @Override
                        public StrategyManager get() {
                            return new Provider<StrategyManager>() {
                                    @Override
                                    public StrategyManager get() {
                                        return myLoadStrategyManager();
                                    }
                                }.get();
                        }
                    }).in(Singleton.class);
				}
            });
		}

		public Scenario getScenario() {
			return this.controler.getScenario() ;
		}

		public void run() {
			this.controler.run();
		}

		private StrategyManager myLoadStrategyManager() {
			return this.manager ;
		}
	}
}
