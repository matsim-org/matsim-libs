/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.run;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.MatsimTestUtils.ExitTrappedException;

/**
 * @author mrieser / Senozon AG
 */
public class CreateFullConfigTest {

	private final static Logger log = Logger.getLogger(CreateFullConfigTest.class);

	@Rule public MatsimTestUtils helper = new MatsimTestUtils();

	@Test
	public void testMain() {
		String[] args = new String[1];
		args[0] = helper.getOutputDirectory() + "newConfig.xml";

		File configFile = new File(args[0]);
		Assert.assertFalse(configFile.exists());

		try {
			MatsimTestUtils.forbidSystemExitCall();
			CreateFullConfig.main(args);
		} finally {
			MatsimTestUtils.enableSystemExitCall();
		}

		Assert.assertTrue(configFile.exists());
	}

	@Test
	public void testMain_MissingArgument() {
		String[] args = new String[0];

		try {
			MatsimTestUtils.forbidSystemExitCall();
			CreateFullConfig.main(args);
			Assert.fail("Expected exception.");
		} catch (ExitTrappedException e) {
			log.info("caught the expected exception, everything is fine.");
		} finally {
			MatsimTestUtils.enableSystemExitCall();
		}

	}
}
