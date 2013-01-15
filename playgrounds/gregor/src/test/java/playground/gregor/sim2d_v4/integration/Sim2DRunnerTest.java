/* *********************************************************************** *
 * project: org.matsim.*
 * Sim2DRunnerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v4.integration;

import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import playground.gregor.sim2d_v4.run.Sim2DRunner;

public class Sim2DRunnerTest extends MatsimTestCase{
	
	@Test
	public void testSim2DRunner() {
		String mconf = getInputDirectory() + "/config.xml";
		String s2dconf = getInputDirectory() + "/s2d_config.xml";
		Sim2DRunner.main(new String[]{s2dconf,mconf, "false"});
		
		assertEquals(false, false);
	}

}
