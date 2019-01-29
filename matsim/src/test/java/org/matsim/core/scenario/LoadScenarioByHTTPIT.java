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
package org.matsim.core.scenario;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author michaz
 *
 */
public class LoadScenarioByHTTPIT {

	@Test
	public void testLoadingScenarioFromURLWorks() throws MalformedURLException {
//		Config config = ConfigUtils.loadConfig(new URL("https://raw.githubusercontent.com/matsim-org/matsimExamples/master/tutorial/lesson-3/config.xml"));
		Config config = ConfigUtils.loadConfig(new URL("https://github.com/matsim-org/matsim/raw/master/examples/scenarios/lesson-3/config.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(config);
		assertThat("Network has expected size.", scenario.getNetwork().getLinks().size(), equalTo(12940));
		assertThat("Population has expected size.", scenario.getPopulation().getPersons().size(), equalTo(8760));
		assertThat("There are the expected number of activity facilities.", scenario.getActivityFacilities().getFacilities().size(), equalTo(10281));
	}

}
