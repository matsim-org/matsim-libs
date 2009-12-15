/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractMultithreadedModuleTest.java
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

package org.matsim.core.replanning.modules;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class AbstractMultithreadedModuleTest extends MatsimTestCase {

	public void testGetNumOfThreads() {
		Config config = super.loadConfig(null);
		config.global().setNumberOfThreads(3);
		DummyAbstractMultithreadedModule testee = new DummyAbstractMultithreadedModule(config.global());
		assertEquals(3, testee.getNumOfThreads());
	}

	private class DummyAbstractMultithreadedModule extends AbstractMultithreadedModule { 

		public DummyAbstractMultithreadedModule(
				GlobalConfigGroup globalConfigGroup) {
			super(globalConfigGroup);
		}
		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return null;
		}

	}

}
