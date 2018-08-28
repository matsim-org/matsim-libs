/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package org.matsim.contrib.transEnergySim.example.car;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunTransEnergySimExampleTest {

	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testRunTransEnegeryExample() {
		String[] args = {testUtils.getOutputDirectory()};
		
		RunTransEnergySimExample.main(args);
		Assert.assertEquals("different event files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "output_events.xml.gz"), 
				CRCChecksum.getCRCFromFile(testUtils.getClassInputDirectory() + "output_events.xml.gz"));
	}

}
