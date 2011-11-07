/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.population;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class MatsimPopulationReaderTest {

	@Test
	public void testReadFile_v4() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimNetworkReader(s).readFile("test/scenarios/equil/network.xml");
		new MatsimPopulationReader(s).readFile("test/scenarios/equil/plans1.xml");
		Assert.assertEquals(1, s.getPopulation().getPersons().size());
	}

	@Test
	public void testReadFile_v5() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimPopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_example.xml");
		Assert.assertEquals(1, s.getPopulation().getPersons().size());
	}
}
