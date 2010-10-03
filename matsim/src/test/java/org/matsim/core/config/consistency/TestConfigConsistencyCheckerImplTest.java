/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.config.consistency;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.testcases.utils.LogCounter;

/**
 * @author mrieser
 */
public class TestConfigConsistencyCheckerImplTest {

	@Test
	public void testCheckTransitReplanningConfiguration() {
		Config config = new Config();
		config.addCoreModules();
		config.scenario().setUseTransit(true);
		config.setParam("strategy", "ModuleProbability_1", "0.9");
		config.setParam("strategy", "Module_1", "TimeAllocationMutator");

		LogCounter logger = new LogCounter(Level.ERROR);
		try {
			logger.activiate();
			Assert.assertEquals(0, logger.getErrorCount());
			new ConfigConsistencyCheckerImpl().checkConsistency(config);
			Assert.assertEquals(1, logger.getErrorCount());
			logger.resetCounts();

			config.setParam("strategy", "Module_1", "ChangeLegMode");
			new ConfigConsistencyCheckerImpl().checkConsistency(config);
			Assert.assertEquals(1, logger.getErrorCount());
			logger.resetCounts();

			config.setParam("strategy", "ModuleProbability_2", "0.9");
			config.setParam("strategy", "Module_2", "TimeAllocationMutator");
			new ConfigConsistencyCheckerImpl().checkConsistency(config);
			Assert.assertEquals(2, logger.getErrorCount());
			logger.resetCounts();

			config.setParam("strategy", "Module_1", "TransitChangeLegMode");
			config.setParam("strategy", "Module_2", "TransitTimeAllocationMutator");
			new ConfigConsistencyCheckerImpl().checkConsistency(config);
			Assert.assertEquals(0, logger.getErrorCount());
		} finally {
			// make sure counter is deactivated at the end
			logger.deactiviate();
		}
	}

}
