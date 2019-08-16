/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura / gleich
 *
 */
public class MainModeIdentifierImplTest {
	private static final Logger log = Logger.getLogger(MainModeIdentifierImplTest.class);

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testIdentifyMainMode() {
		log.info("Running testIdentifyMainMode");

		MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationFactory factory = scenario.getPopulation().getFactory();

		{
			// A normal pt trip
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("pt interaction", null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId("pt interaction", null));
			planElements.add(factory.createLeg(TransportMode.pt));
			planElements.add(factory.createActivityFromLinkId("pt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.pt, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			// No pt route found -> only a "transit_walk" leg, but main mode is pt
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.transit_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.pt, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			// Special case: Closest stop to origin and closest stop to destination are identical
			// No pt leg, but a stage activity which should be used to identify the main mode.
			// (see MATSIM-943, MATSIM-945).
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("pt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.pt, mainModeIdentifier.identifyMainMode(planElements));
		}

		{
			// a normal network mode trip without access/egress walk legs
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.car));
			Assert.assertEquals("Wrong mode!", TransportMode.car, mainModeIdentifier.identifyMainMode(planElements));
		}

		{
			// a normal drt trip without access/egress walk legs
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.drt));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.drt, mainModeIdentifier.identifyMainMode(planElements));
		}
		
		{
			// Special case: Closest drt link to origin and closest drt link to destination are identical
			// No drt leg, but a stage activity which should be used to identify the main mode.
			// (see MATSIM-943, MATSIM-945).
			List<PlanElement> planElements = new ArrayList<>();
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			planElements.add(factory.createActivityFromLinkId("drt interaction", null));
			planElements.add(factory.createLeg(TransportMode.non_network_walk));
			Assert.assertEquals("Wrong mode!", TransportMode.drt, mainModeIdentifier.identifyMainMode(planElements));
		}

		log.info("Running testIdentifyMainMode Done.");

	}
}