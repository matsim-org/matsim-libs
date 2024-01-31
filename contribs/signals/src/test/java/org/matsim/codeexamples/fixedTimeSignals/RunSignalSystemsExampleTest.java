/* *********************************************************************** *
 * project: org.matsim.*												   *
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
package org.matsim.codeexamples.fixedTimeSignals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunSignalSystemsExampleTest {

	@RegisterExtension private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void testExampleWithHoles() {
		boolean usingOTFVis = false ;
		try {
			RunSignalSystemsExampleWithHoles.run(usingOTFVis);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assertions.fail("something went wrong: " + ee.getMessage()) ;
		}
	}

	@Test
	final void testMinimalExample() {
		try {
			Config config = ConfigUtils.loadConfig("./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/config.xml");
			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(testUtils.getOutputDirectory());

			RunSignalSystemsExample.run(config, false);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assertions.fail("something went wrong: " + ee.getMessage()) ;
		}
	}

}
