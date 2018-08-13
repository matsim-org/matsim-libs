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
package tutorial.fixedTimeSignals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunSignalSystemsExampleTest {

	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void testExampleWithHoles() {
		boolean usingOTFVis = false ;
		try {
			RunSignalSystemsExampleWithHoles.run(usingOTFVis);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong: " + ee.getMessage()) ;
		}
	}
	
	@Test
	public final void testMinimalExample() {
		try {
			Config config = ConfigUtils.loadConfig("./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/config.xml");
			config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controler().setLastIteration(0);
			config.controler().setOutputDirectory(testUtils.getOutputDirectory());
			
			RunSignalSystemsExample.run(config, false);
		} catch (Exception ee ) {
			ee.printStackTrace();
			Assert.fail("something went wrong: " + ee.getMessage()) ;
		}
	}

}
