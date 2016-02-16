/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.contrib.noise;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.data.GridParameters;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class NoiseConfigGroupTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config1.xml";
		Config config = ConfigUtils.loadConfig(configFile, new GridParameters());
				
		GridParameters gridParameters = (GridParameters) config.getModule("grid");
		System.out.println(gridParameters.getReceiverPointGap());

		Assert.assertEquals("wrong config parameter", 12345., gridParameters.getReceiverPointGap(), MatsimTestUtils.EPSILON);		

//		GridParameters grid = ConfigUtils.addOrGetModule(config, GridParameters.GROUP_NAME, GridParameters.class);
//		grid.setReceiverPointGap(250.);
				
//		GridParameters gridParameters = new GridParameters();
//		gridParameters.setReceiverPointGap(250.);	
	}
		
}
