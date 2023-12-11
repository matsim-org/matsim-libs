/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.av.intermodal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author jbischoff
 */
public class RunTaxiPTIntermodalExampleIT {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIntermodalExample() throws MalformedURLException {
		URL configUrl = new File(utils.getClassInputDirectory() + "config.xml").toURI().toURL();
		new RunTaxiPTIntermodalExample().run(configUrl, false);

		// check for intermodal trips
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader reader = new PopulationReader(scenario);
		reader.readFile("./output/intermodalExample/output_plans.xml.gz");

		int intermodalTripCounter = 0;

		for (Person person : scenario.getPopulation().getPersons().values()) {
			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());

			for (Trip trip : trips) {
				Map<String, Integer> mode2NumberOfLegs = new HashMap<>();
				for (Leg leg : trip.getLegsOnly()) {
					if (!mode2NumberOfLegs.containsKey(leg.getMode())) {
						mode2NumberOfLegs.put(leg.getMode(), 1);
					} else {
						mode2NumberOfLegs.put(leg.getMode(), mode2NumberOfLegs.get(leg.getMode()) + 1);
					}
				}
				if (mode2NumberOfLegs.containsKey(TransportMode.taxi) && mode2NumberOfLegs.containsKey(
						TransportMode.pt)) {
					intermodalTripCounter++;
				}
			}
		}

		Assertions.assertTrue(intermodalTripCounter > 0,
				"no pt agent has any intermodal route (=taxi for access or egress to pt)");
	}
}
