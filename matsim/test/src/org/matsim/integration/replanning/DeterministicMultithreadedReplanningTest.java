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

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.ReRoute;
import org.matsim.replanning.modules.TimeAllocationMutator;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

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
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(5);
		config.global().setNumberOfThreads(4); // just use any number > 1

		PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
		strategy.addStrategyModule(new TimeAllocationMutator());
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.addStrategy(strategy, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		new TestControler(config, strategyManager).run();

		Gbl.reset();
		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		new TestControler(config, strategyManager).run();

		long cksum1 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run1/ITERS/it.5/5.events.txt.gz");
		long cksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run2/ITERS/it.5/5.events.txt.gz");

		assertEquals("The checksums of events must be the same, even when multiple threads are used.", cksum1, cksum2);
	}

	/**
	 * Tests that the generic {@link ReRoute} generates always the same results
	 * with the same number of threads.
	 */
	public void testReRoute() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(5);
		config.global().setNumberOfThreads(4); // just use any number > 1

		// setup run1
		PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
		StrategyManager strategyManager = new StrategyManager();
		strategyManager.addStrategy(strategy, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		Controler controler = new TestControler(config, strategyManager);
		strategy.addStrategyModule(new ReRoute(controler));
		controler.run();

		// setup run2
		Gbl.reset();
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		StrategyManager strategyManager2 = new StrategyManager();
		strategyManager2.addStrategy(strategy2, 1.0);

		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		Controler controler2 = new TestControler(config, strategyManager2);
		strategy2.addStrategyModule(new ReRoute(controler));
		controler2.run();

		long cksum1 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run1/ITERS/it.5/5.events.txt.gz");
		long cksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run2/ITERS/it.5/5.events.txt.gz");

		assertEquals("The checksums of events must be the same, even when multiple threads are used.", cksum1, cksum2);
	}

	/**
	 * A simple Controler for the tests above to overwrite the StrategyManager.
	 *
	 * @author mrieser
	 */
	private static class TestControler extends Controler {
		private final StrategyManager strategyManager;

		public TestControler(final Config config, final StrategyManager manager) {
			super(config);
			this.strategyManager = manager;
			this.setCreateGraphs(false);
		}

		@Override
		protected StrategyManager loadStrategyManager() {
			return this.strategyManager;
		}
	}
}
