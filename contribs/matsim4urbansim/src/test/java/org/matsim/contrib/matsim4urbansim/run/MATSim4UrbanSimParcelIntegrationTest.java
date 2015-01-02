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
package org.matsim.contrib.matsim4urbansim.run;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.matrixbasedptrouter.utils.CreateTestNetwork;
import org.matsim.contrib.matsim4urbansim.config.CreateTestM4UConfig;
import org.matsim.contrib.matsim4urbansim.utils.CreateTestUrbansimPopulation;
import org.matsim.core.api.experimental.network.NetworkWriter;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class MATSim4UrbanSimParcelIntegrationTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test

	public void test() {
		String path = utils.getOutputDirectory() ;
		
		CreateTestUrbansimPopulation.createUrbanSimTestPopulation(path, 1);
		
		NetworkWriter writer = new NetworkWriter( CreateTestNetwork.createTestNetwork() ) ;
		final String networkFilename = path + "/network.xml.gz";
		writer.write( networkFilename);
		
		CreateTestM4UConfig creator = new CreateTestM4UConfig(path, networkFilename );
		String result = creator.generateConfigV3() ;
		
		String[] args = { result } ;
		MATSim4UrbanSimParcel.main( args ); 

	}

}
