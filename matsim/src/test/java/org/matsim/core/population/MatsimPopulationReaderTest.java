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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
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
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assert.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assert.assertEquals(3, planElements.size());
		
		Assert.assertTrue(planElements.get(0) instanceof Activity);
		Assert.assertTrue(planElements.get(1) instanceof Leg);
		Assert.assertTrue(planElements.get(2) instanceof Activity);
	}
	
	@Test
	public void testReadFile_v5_multipleSuccessiveLegs() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimPopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleLegs.xml");
		Assert.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assert.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assert.assertEquals(5, planElements.size());
		
		Assert.assertTrue(planElements.get(0) instanceof Activity);
		Assert.assertTrue(planElements.get(1) instanceof Leg);
		Assert.assertTrue(planElements.get(2) instanceof Leg);
		Assert.assertTrue(planElements.get(3) instanceof Leg);
		Assert.assertTrue(planElements.get(4) instanceof Activity);
	}
		
	@Test
	public void testReadFile_v5_multipleSuccessiveLegsWithRoutes() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimPopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleLegsWithRoutes.xml");
		Assert.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assert.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assert.assertEquals(5, planElements.size());
		
		Assert.assertTrue(planElements.get(0) instanceof Activity);
		Assert.assertTrue(planElements.get(1) instanceof Leg);
		Assert.assertEquals(Id.create("1", Link.class), ((Leg) planElements.get(1)).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("2", Link.class), ((Leg) planElements.get(1)).getRoute().getEndLinkId());
		Assert.assertTrue(planElements.get(2) instanceof Leg);
		Assert.assertEquals(Id.create("2", Link.class), ((Leg) planElements.get(2)).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("4", Link.class), ((Leg) planElements.get(2)).getRoute().getEndLinkId());
		Assert.assertTrue(planElements.get(3) instanceof Leg);
		Assert.assertEquals(Id.create("4", Link.class), ((Leg) planElements.get(3)).getRoute().getStartLinkId());
		Assert.assertEquals(Id.create("6", Link.class), ((Leg) planElements.get(3)).getRoute().getEndLinkId());
		Assert.assertTrue(planElements.get(4) instanceof Activity);
	}
	
	@Test
	public void testReadFile_v5_multipleSuccessiveLegsWithTeleportation() {
		Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Assert.assertEquals(0, s.getPopulation().getPersons().size());
		new MatsimPopulationReader(s).readFile("test/input/org/matsim/core/utils/io/MatsimFileTypeGuesserTest/population_v5_multipleTeleportedLegs.xml");
		Assert.assertEquals(1, s.getPopulation().getPersons().size());
		Person person = s.getPopulation().getPersons().get(Id.create(1, Person.class));
		Assert.assertNotNull(person);
		Plan plan = person.getSelectedPlan();
		List<PlanElement> planElements = plan.getPlanElements();
		Assert.assertEquals(5, planElements.size());
		
		Assert.assertTrue(planElements.get(0) instanceof Activity);
		Assert.assertTrue(planElements.get(1) instanceof Leg);
		Assert.assertTrue(((Leg) planElements.get(1)).getRoute() instanceof GenericRouteImpl); 
		Assert.assertTrue(planElements.get(2) instanceof Leg);
		Assert.assertTrue(((Leg) planElements.get(2)).getRoute() instanceof NetworkRoute); 
		Assert.assertTrue(planElements.get(3) instanceof Leg);
		Assert.assertTrue(((Leg) planElements.get(3)).getRoute() instanceof GenericRouteImpl); 
		Assert.assertTrue(planElements.get(4) instanceof Activity);
	}
}
