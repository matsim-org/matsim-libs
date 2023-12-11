/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatConfigGroupTest.java
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

package org.matsim.core.config.groups;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

public class VspExperimentalConfigGroupTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(VspExperimentalConfigGroupTest.class);

	@Test
	void testVspConfigGroup() {

//		VspExperimentalConfigGroup vspConfig = ConfigUtils.createConfig().vspExperimental() ;
//
//		vspConfig.setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.WARN) ;
//		// this should (just) produce warning messages:
//		vspConfig.checkConsistency() ;
//
//		vspConfig.setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.ABORT) ;
//		try {
//			// should throw RuntimeException:
//			vspConfig.checkConsistency() ;
//			fail("should never get here since it should have thrown an exception before") ;
//		} catch ( RuntimeException e ) {
//			log.info("Caught RuntimeException, as expected: " + e.getMessage());
//		}

		// this type of checking level is only at the level of the config group itself, which is too low
		// for many case that I need.
		// In consequence, also the test is not very useful --> commenting it out; might be deleted eventually.
		// kai, oct'12
	}

}
