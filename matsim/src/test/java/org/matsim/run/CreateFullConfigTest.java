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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser / Senozon AG
 */
public class CreateFullConfigTest {

	private final static Logger log = LogManager.getLogger(CreateFullConfigTest.class);

	@RegisterExtension private MatsimTestUtils helper = new MatsimTestUtils();

	@Test
	void testMain() {
		String[] args = new String[1];
		args[0] = helper.getOutputDirectory() + "newConfig.xml";

		File configFile = new File(args[0]);
		Assertions.assertFalse(configFile.exists());

		CreateFullConfig.main(args);

		Assertions.assertTrue(configFile.exists());
	}
}
