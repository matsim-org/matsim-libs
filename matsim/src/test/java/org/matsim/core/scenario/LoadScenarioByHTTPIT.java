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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author michaz
 *
 */
public class LoadScenarioByHTTPIT {

	@Test
	void testLoadingScenarioFromURLWorks() throws MalformedURLException {
//		Config config = ConfigUtils.loadConfig(new URL("https://raw.githubusercontent.com/matsim-org/matsimExamples/master/tutorial/lesson-3/config.xml"));
		Config config = ConfigUtils.loadConfig(new URL("https://github.com/matsim-org/matsim/raw/master/examples/scenarios/lesson-3/config.xml"));
		Scenario scenario = ScenarioUtils.loadScenario(config);
		assertThat(scenario.getNetwork().getLinks()).hasSize(12940);
		assertThat(scenario.getPopulation().getPersons()).hasSize(8760);
		assertThat(scenario.getActivityFacilities().getFacilities()).hasSize(10281);
	}

}
