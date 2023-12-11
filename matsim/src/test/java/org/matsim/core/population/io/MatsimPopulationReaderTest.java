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

package org.matsim.core.population.io;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class MatsimPopulationReaderTest {

	@Test
	void testReadFile_v4() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimNetworkReader(s.getNetwork()).readFile("test/scenarios/equil/network.xml");
		new PopulationReader(s).readFile("test/scenarios/equil/plans1.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
	}

	@Test
	void testReadFile_v5() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(0, s.getPopulation().getPersons().size());
		new PopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_example.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assertions.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assertions.assertEquals(3, planElements.size());
		
		Assertions.assertTrue(planElements.get(0) instanceof Activity);
		Assertions.assertTrue(planElements.get(1) instanceof Leg);
		Assertions.assertTrue(planElements.get(2) instanceof Activity);
	}

	@Test
	void testReadFile_v5_multipleSuccessiveLegs() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(0, s.getPopulation().getPersons().size());
		new PopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleLegs.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assertions.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assertions.assertEquals(5, planElements.size());
		
		Assertions.assertTrue(planElements.get(0) instanceof Activity);
		Assertions.assertTrue(planElements.get(1) instanceof Leg);
		Assertions.assertTrue(planElements.get(2) instanceof Leg);
		Assertions.assertTrue(planElements.get(3) instanceof Leg);
		Assertions.assertTrue(planElements.get(4) instanceof Activity);
	}

	@Test
	void testReadFile_v5_multipleSuccessiveLegsWithRoutes() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(0, s.getPopulation().getPersons().size());
		new PopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleLegsWithRoutes.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assertions.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assertions.assertEquals(5, planElements.size());
		
		Assertions.assertTrue(planElements.get(0) instanceof Activity);
		Assertions.assertTrue(planElements.get(1) instanceof Leg);
		Assertions.assertEquals(Id.create("1", Link.class), ((Leg) planElements.get(1)).getRoute().getStartLinkId());
		Assertions.assertEquals(Id.create("2", Link.class), ((Leg) planElements.get(1)).getRoute().getEndLinkId());
		Assertions.assertTrue(planElements.get(2) instanceof Leg);
		Assertions.assertEquals(Id.create("2", Link.class), ((Leg) planElements.get(2)).getRoute().getStartLinkId());
		Assertions.assertEquals(Id.create("4", Link.class), ((Leg) planElements.get(2)).getRoute().getEndLinkId());
		Assertions.assertTrue(planElements.get(3) instanceof Leg);
		Assertions.assertEquals(Id.create("4", Link.class), ((Leg) planElements.get(3)).getRoute().getStartLinkId());
		Assertions.assertEquals(Id.create("6", Link.class), ((Leg) planElements.get(3)).getRoute().getEndLinkId());
		Assertions.assertTrue(planElements.get(4) instanceof Activity);
	}

	@Test
	void testReadFile_v5_multipleSuccessiveLegsWithTeleportation() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assertions.assertEquals(0, s.getPopulation().getPersons().size());
		new PopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleTeleportedLegs.xml");
		Assertions.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assertions.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assertions.assertEquals(5, planElements.size());
		
		Assertions.assertTrue(planElements.get(0) instanceof Activity);
		Assertions.assertTrue(planElements.get(1) instanceof Leg);
//		Assert.assertTrue(((Leg) planElements.get(1)).getRoute() instanceof GenericRouteImpl); 
		Assertions.assertTrue(planElements.get(2) instanceof Leg);
		Assertions.assertTrue(((Leg) planElements.get(2)).getRoute() instanceof NetworkRoute); 
		Assertions.assertTrue(planElements.get(3) instanceof Leg);
//		Assert.assertTrue(((Leg) planElements.get(3)).getRoute() instanceof GenericRouteImpl); 
		Assertions.assertTrue(planElements.get(4) instanceof Activity);
	}
}
