/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractMultithreadedModuleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008, 2011 by the members listed in the COPYING,  *
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

package org.matsim.core.replanning.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;

/**
 * @author mrieser
 */
public class AbstractMultithreadedModuleTest {

	private final static Logger log = LogManager.getLogger(AbstractMultithreadedModuleTest.class);

	@Test
	void testGetNumOfThreads() {
		Config config = new Config();
		config.addCoreModules();
		config.global().setNumberOfThreads(3);
		DummyAbstractMultithreadedModule testee = new DummyAbstractMultithreadedModule(config.global());
		Assertions.assertEquals(3, testee.getNumOfThreads());
	}

	@Test
	void testCrashingThread() {
		try {
			DummyCrashingModule testee = new DummyCrashingModule(2);
			testee.prepareReplanning(null);
			testee.handlePlan(null);
			testee.handlePlan(null);
			testee.handlePlan(null);
			testee.finishReplanning();
			Assertions.fail("expected exception, got none.");
		} catch (Exception e) {
			log.info("Catched expected exception.", e);
		}

	}

	private static class DummyAbstractMultithreadedModule extends AbstractMultithreadedModule {
		public DummyAbstractMultithreadedModule(GlobalConfigGroup globalConfigGroup) {
			super(globalConfigGroup);
		}
		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return null;
		}
	}

	private static class DummyCrashingModule extends AbstractMultithreadedModule {
		public DummyCrashingModule(final int nOfThreads) {
			super(nOfThreads);
		}
		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return new CrashingPlanAlgo();
		}
	}

	private static class CrashingPlanAlgo implements PlanAlgorithm {
		@Override
		public void run(Plan plan) {
			throw new IllegalArgumentException("just some exception to crash this thread.");
		}
	}
}
