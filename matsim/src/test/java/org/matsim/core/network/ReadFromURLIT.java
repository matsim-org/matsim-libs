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

import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author nagel
 * @author michaz
 *
 */
public class ReadFromURLIT {

	@Test
	public void testReadingFromURLWorks() throws MalformedURLException {
		Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork() ;
		MatsimNetworkReader reader = new MatsimNetworkReader(network) ;
//		reader.parse(new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/matsim/examples/equil/network.xml"));
		reader.parse( new URL("https://raw.githubusercontent.com/matsim-org/matsim/master/examples/scenarios/equil/network.xml") ) ;
		assertThat(network.getLinks().size(), equalTo(23));
	}

}
