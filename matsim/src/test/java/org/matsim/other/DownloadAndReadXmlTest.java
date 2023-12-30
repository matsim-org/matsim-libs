/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.other;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * Tests downloading and reading in xml files from our public svn.
 *
* @author gleich
*/

public class DownloadAndReadXmlTest {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	@Disabled
	/**
	 * Http downloads from the SVN server will be forbidden soon, according to jwilk.
	 */
	@Test final void testHttpFromSvn() {

		Config config = ConfigUtils.createConfig();
		System.out.println(utils.getInputDirectory() + "../../");
		config.network().setInputFile("http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/input/network.xml");

		// See whether the file can be downloaded and read
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Check whether all nodes and links were read
		Network network = scenario.getNetwork();

		// 3 pt nodes and 4 x 4 car nodes
		Assertions.assertEquals(3 + 4 * 4, network.getNodes().size());

		// 6 pt links and 3 links * 2 directions * 4 times in parallel * 2 (horizontally and vertically)
		Assertions.assertEquals(6 + 3 * 2 * 4 * 2, network.getLinks().size());
	}

	@Test
	final void testHttpsFromSvn() {

		Config config = ConfigUtils.createConfig();
		System.out.println(utils.getInputDirectory() + "../../");
		config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/atlantis/minibus/input/network.xml");

		// See whether the file can be downloaded and read
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Check whether all nodes and links were read
		Network network = scenario.getNetwork();

		// 3 pt nodes and 4 x 4 car nodes
		Assertions.assertEquals(3 + 4 * 4, network.getNodes().size());

		// 6 pt links and 3 links * 2 directions * 4 times in parallel * 2 (horizontally and vertically)
		Assertions.assertEquals(6 + 3 * 2 * 4 * 2, network.getLinks().size());
	}

}

