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

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

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
public class DeterministicMultithreadedReplanningTest extends MatsimTestCase {

	/**
	 * Tests that the {@link TimeAllocationMutator} generates always the same results
	 * with the same number of threads.
	 */
	public void testTimeAllocationMutator() {
		int lastIteration = 5;
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1

		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new TimeAllocationMutator(config));
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		new TestControler(config, strategyManager).run();

		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		new TestControler(config, strategyManager).run();

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}

	/**
	 * Tests that the combination of {@link ReRoute} and {@link TimeAllocationMutator} always generates 
	 * the same results with the same number of threads.
	 */
	public void testReRouteTimeAllocationMutator() {
		int lastIteration = 5;
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1
		
		// setup run1
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.setMaxPlansPerAgent(5);
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);
		
		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		TestControler controler = new TestControler(config, strategyManager);
		strategy.addStrategyModule(new ReRoute(controler.getScenario())); // finish strategy configuration
		strategy.addStrategyModule(new TimeAllocationMutator(config));
		controler.run();

		// setup run2
		StrategyManager strategyManager2 = new StrategyManager();
		strategyManager2.setMaxPlansPerAgent(5);
		PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
		strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);
		
		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		TestControler controler2 = new TestControler(config, strategyManager2);
		strategy2.addStrategyModule(new ReRoute(controler2.getScenario())); // finish strategy configuration
		strategy2.addStrategyModule(new TimeAllocationMutator(config));
		controler2.run();

		for (int i = 0; i <= lastIteration; i++) {
			
			long cksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			
			assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}
		
		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}
	
	/**
	 * Tests that the generic {@link ReRoute} generates always the same results
	 * with the same number of threads using only one agent.
	 */
	public void testReRouteOneAgent() {
		int lastIteration = 5;

		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1
		config.plans().setInputFile(this.getClassInputDirectory() + "plans1.xml");

		// setup run1
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		Controler controler = new TestControler(config, strategyManager);
		strategy.addStrategyModule(new ReRoute(controler.getScenario()));
		controler.run();

		// setup run2
		PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
		StrategyManager strategyManager2 = new StrategyManager();
		strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		config.global().setNumberOfThreads(3); // use a different number of threads because the result must be the same
		Controler controler2 = new TestControler(config, strategyManager2);
		strategy2.addStrategyModule(new ReRoute(controler.getScenario()));

		controler2.run();

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}

	}

	/**
	 * Tests that the generic {@link ReRoute} generates always the same results
	 * with the same number of threads.
	 */
	public void testReRoute() {
		int lastIteration = 5;
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(lastIteration);
		config.global().setNumberOfThreads(4); // just use any number > 1


		// setup run1
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.addStrategyForDefaultSubpopulation(strategy, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		Controler controler = new TestControler(config, strategyManager);
		strategy.addStrategyModule(new ReRoute(controler.getScenario()));
		controler.run();

		// setup run2
		PlanStrategyImpl strategy2 = new PlanStrategyImpl(new RandomPlanSelector());
		StrategyManager strategyManager2 = new StrategyManager();
		strategyManager2.addStrategyForDefaultSubpopulation(strategy2, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		config.global().setNumberOfThreads(3); // use a different number of threads because the result must be the same
		Controler controler2 = new TestControler(config, strategyManager2);
		strategy2.addStrategyModule(new ReRoute(controler.getScenario()));
		controler2.run();

		for (int i = 0; i <= lastIteration; i++) {

			long cksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".events.xml.gz");
			long cksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".events.xml.gz");

			assertEquals("The checksums of events must be the same in iteration " + i + ", even when multiple threads are used.", cksum1, cksum2);
		}

		for (int i = 0; i < 2; i++) {
			long pcksum1 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run1/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			long pcksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "/run2/ITERS/it."+ i +"/"+ i +".plans.xml.gz");
			assertEquals("The checksums of plans must be the same in iteration " + i + ", even when multiple threads are used.", pcksum1, pcksum2);
		}
	}

	/**
	 * A simple Controler for the tests above to overwrite the StrategyManager.
	 *
	 * @author mrieser
	 */
	private static class TestControler extends Controler {

		private StrategyManager manager;

		public TestControler(final Config config, final StrategyManager manager) {
			super(config);
            this.getConfig().controler().setCreateGraphs(false);
            this.getConfig().controler().setWriteEventsInterval(1);
			this.setDumpDataAtEnd(false);
			this.manager = manager ;
            addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bindToProviderAsSingleton(StrategyManager.class, new Provider<StrategyManager>() {
                        @Override
                        public StrategyManager get() {
                            return myLoadStrategyManager();
                        }
                    });
                }
            });
		}

		private StrategyManager myLoadStrategyManager() {
			return this.manager ;
		}
	}
}
