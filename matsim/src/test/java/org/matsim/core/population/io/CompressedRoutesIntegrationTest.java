/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.population.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / senozon
 */
public class CompressedRoutesIntegrationTest {

	@Test
	void testReadingPlansV4parallel() {
		Config config = ConfigUtils.createConfig();
		config.plans().setNetworkRouteType("CompressedNetworkRoute");
		Scenario s = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(s.getNetwork()).readFile("test/scenarios/equil/network.xml");
		new ParallelPopulationReaderMatsimV4(s).readFile("test/scenarios/equil/plans1.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
		Leg firstPersonsLeg = (Leg) s.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan().getPlanElements().get(1);
//		Assert.assertTrue(firstPersonsLeg.getRoute() instanceof CompressedNetworkRouteImpl);
	}
	
}
