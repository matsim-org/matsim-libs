/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyConfigGroupTest.java
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

package org.matsim.config.groups;

import org.apache.log4j.Logger;
import org.matsim.testcases.MatsimTestCase;

/**
 * Test for {@link StrategyConfigGroup}.
 *
 * @author mrieser
 */
public class StrategyConfigGroupTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(StrategyConfigGroupTest.class);

	/**
	 * Tests that only the known param-names are accepted, and no others.
	 *
	 * @author mrieser
	 */
	public void testParamNames() {
		StrategyConfigGroup configGroup = new StrategyConfigGroup();
		configGroup.addParam("maxAgentPlanMemorySize", "3");
		configGroup.addParam("Module_1", "ReRoute");
		configGroup.addParam("ModuleProbability_1", "0.5");
		configGroup.addParam("ModuleDisableAfterIteration_1", "20");
		try {
			configGroup.addParam("ModuleWrong_1", "should fail");
			fail("Expected to get an IllegalArgumentException, but got none.");
		}
		catch (IllegalArgumentException e) {
			log.info("Catched IllegalArgumentException, as exptected: "  + e.getMessage());
		}
	}

	/**
	 * Tests that inconsistent configuration states are recognized, like
	 * missing settings or wrong enumeration.
	 *
	 * @author mrieser
	 */
	public void testCheckConsistency() {
		// start with a simple configuration with exactly one module defined
		StrategyConfigGroup configGroup = new StrategyConfigGroup();
		configGroup.addParam("maxAgentPlanMemorySize", "3");
		configGroup.addParam("Module_1", "ReRoute");
		configGroup.addParam("ModuleProbability_1", "0.5");
		configGroup.addParam("ModuleDisableAfterIteration_1", "20");
		configGroup.checkConsistency();

		// add a 2nd module
		configGroup.addParam("Module_2", "TimeAllocationMutator");
		configGroup.addParam("ModuleProbability_2", "0.4");
		configGroup.checkConsistency();

		// add a 3rd module, but inconsistent with the enumeration
		configGroup.addParam("Module_4", "SelectRandom");
		configGroup.addParam("ModuleProbability_4", "0.3");
		try {
			configGroup.checkConsistency();
			fail("Expected to fail consistency check with missing Module_3, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}

		// fix the configuration by adding the missing module
		configGroup.addParam("Module_3", "KeepLastSelected");
		configGroup.addParam("ModuleProbability_3", "0.2");
		configGroup.checkConsistency();

		// break the configuration by adding an incomplete module
		configGroup.addParam("Module_5", "SelectBest");
		try {
			configGroup.checkConsistency();
			fail("Expected to fail consistency check with incomplete Module_5, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}

		// fix Module_5
		configGroup.addParam("ModuleProbability_5", "0.0");
		configGroup.checkConsistency();

		// add another incomplete module
		configGroup.addParam("ModuleProbability_6", "0.1");
		try {
			configGroup.checkConsistency();
			fail("Expected to fail consistency check with incomplete Module_6, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}

		// fix Module_6
		configGroup.addParam("Module_6", "SelectExpBeta");
		configGroup.checkConsistency();

		// add forbidden Module_0
		configGroup.addParam("Module_0", "ChangeExpBeta");
		configGroup.addParam("ModuleProbability_0", "0.6");
		try {
			configGroup.checkConsistency();
			fail("Expected to fail consistency check with Module_0, but did not fail.");
		} catch (RuntimeException e) {
			log.info("Catched RuntimeException, as expected: " + e.getMessage());
		}
	}

}
