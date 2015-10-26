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
package org.matsim.core.network;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class NetworkUtilsTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link org.matsim.core.network.NetworkUtils#isMultimodal(org.matsim.api.core.v01.network.Network)}.
	 */
	@Test
	public final void testIsMultimodal() {

		Config config = ConfigUtils.createConfig() ;
		config.network().setInputFile( utils.getInputDirectory() + "/network.xml" );
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		Network network = scenario.getNetwork() ;
		
		Assert.assertTrue( NetworkUtils.isMultimodal( network ) );
		
	}

}
