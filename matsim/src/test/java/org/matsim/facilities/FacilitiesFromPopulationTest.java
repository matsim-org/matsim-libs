/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.facilities;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class FacilitiesFromPopulationTest {

	@Test
	void testRun_onePerLink_assignLinks() {
		Fixture f = new Fixture();

		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(true);
		generator.setAssignLinksToFacilitiesIfMissing( f.scenario.getNetwork() );
		generator.run(f.scenario.getPopulation());

		Assertions.assertEquals(3, f.scenario.getActivityFacilities().getFacilities().size());

		Assertions.assertEquals("bc", f.scenario.getActivityFacilities().getFacilities().get(Id.create("bc", ActivityFacility.class)).getLinkId().toString());
		Assertions.assertEquals("ca", f.scenario.getActivityFacilities().getFacilities().get(Id.create("ca", ActivityFacility.class)).getLinkId().toString());
		Assertions.assertEquals("ab", f.scenario.getActivityFacilities().getFacilities().get(Id.create("ab", ActivityFacility.class)).getLinkId().toString());

		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("2", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("3", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("4", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("5", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("6", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("7", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("8", Person.class)).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("9", Person.class)).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("0", Person.class)).getSelectedPlan(), "ca", "ab", true);
	}

	@Test
	void testRun_onePerLink_assignLinks_openingTimes() {
		Fixture f = new Fixture();

		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(true);
		generator.setAddEmptyActivityOptions(true);
		generator.setAssignLinksToFacilitiesIfMissing( f.scenario.getNetwork() );
		ScoringConfigGroup config = new ScoringConfigGroup();
		ActivityParams homeParams = new ActivityParams("home");
		ActivityParams workParams = new ActivityParams("work");
		workParams.setOpeningTime(7*3600);
		workParams.setClosingTime(19*3600);
		config.addActivityParams(homeParams);
		config.addActivityParams(workParams);
		generator.assignOpeningTimes( config );
		generator.run(f.scenario.getPopulation());

		Assertions.assertEquals(3, f.scenario.getActivityFacilities().getFacilities().size());

		Map<Id<ActivityFacility>, ? extends ActivityFacility> ffs = f.scenario.getActivityFacilities().getFacilities();
		Assertions.assertEquals(7*3600, ffs.get(Id.create("ab", ActivityFacility.class)).getActivityOptions().get("work").getOpeningTimes().first().getStartTime(), 1e-7);
		Assertions.assertEquals(19*3600, ffs.get(Id.create("ab", ActivityFacility.class)).getActivityOptions().get("work").getOpeningTimes().first().getEndTime(), 1e-7);

		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("2", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("3", Person.class)).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("4", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("5", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("6", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("7", Person.class)).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("8", Person.class)).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("9", Person.class)).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("0", Person.class)).getSelectedPlan(), "ca", "ab", true);
	}

	@Test
	void testRun_multiple_assignLinks() {
		Fixture f = new Fixture();

		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(false);
		generator.setAssignLinksToFacilitiesIfMissing( f.scenario.getNetwork() );
		generator.run(f.scenario.getPopulation());

//		for (ActivityFacility af : f.scenario.getActivityFacilities().getFacilities().values()) {
//			System.out.println(af.getId() + "\t" + af.getLinkId() + "\t" + af.getCoord().getX() + "\t" + af.getCoord().getY());
//		}

		Assertions.assertEquals(13, f.scenario.getActivityFacilities().getFacilities().size());

		Map<Id<ActivityFacility>, ? extends ActivityFacility> ffs = f.scenario.getActivityFacilities().getFacilities();
		Assertions.assertEquals("ab", ffs.get(Id.create("0", ActivityFacility.class)).getLinkId().toString()); // home of agent 1
		Assertions.assertEquals("bc", ffs.get(Id.create("1", ActivityFacility.class)).getLinkId().toString()); // work of agent 1-3
		Assertions.assertEquals("ab", ffs.get(Id.create("2", ActivityFacility.class)).getLinkId().toString()); // home of agent 2
		Assertions.assertEquals("ab", ffs.get(Id.create("3", ActivityFacility.class)).getLinkId().toString()); // home of agent 3
		Assertions.assertEquals("bc", ffs.get(Id.create("4", ActivityFacility.class)).getLinkId().toString()); // home of agent 4
		Assertions.assertEquals("ca", ffs.get(Id.create("5", ActivityFacility.class)).getLinkId().toString()); // work of agent 4-7
		Assertions.assertEquals("bc", ffs.get(Id.create("6", ActivityFacility.class)).getLinkId().toString()); // home of agent 5
		Assertions.assertEquals("bc", ffs.get(Id.create("7", ActivityFacility.class)).getLinkId().toString()); // home of agent 6
		Assertions.assertEquals("bc", ffs.get(Id.create("8", ActivityFacility.class)).getLinkId().toString()); // home of agent 7
		Assertions.assertEquals("ca", ffs.get(Id.create("9", ActivityFacility.class)).getLinkId().toString()); // home of agent 8
		Assertions.assertEquals("ab", ffs.get(Id.create("10", ActivityFacility.class)).getLinkId().toString()); // work of agent 8-10
		Assertions.assertEquals("ca", ffs.get(Id.create("11", ActivityFacility.class)).getLinkId().toString()); // home of agent 9
		Assertions.assertEquals("ca", ffs.get(Id.create("12", ActivityFacility.class)).getLinkId().toString()); // home of agent 10

		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan(), "0", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("2", Person.class)).getSelectedPlan(), "2", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("3", Person.class)).getSelectedPlan(), "3", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("4", Person.class)).getSelectedPlan(), "4", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("5", Person.class)).getSelectedPlan(), "6", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("6", Person.class)).getSelectedPlan(), "7", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("7", Person.class)).getSelectedPlan(), "8", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("8", Person.class)).getSelectedPlan(), "9", "10", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("9", Person.class)).getSelectedPlan(), "11", "10", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(Id.create("0", Person.class)).getSelectedPlan(), "12", "10", true);
	}

	private void assertPlan(Plan plan, String homeFacilityId, String workFacilityId, boolean linkCoordMustBeNull) {
		Activity home1 = (Activity) plan.getPlanElements().get(0);
		Activity work = (Activity) plan.getPlanElements().get(2);
		Activity home2 = (Activity) plan.getPlanElements().get(4);

		Assertions.assertEquals(homeFacilityId, home1.getFacilityId().toString());
		Assertions.assertEquals(workFacilityId, work.getFacilityId().toString());
		Assertions.assertEquals(homeFacilityId, home2.getFacilityId().toString());

		if (linkCoordMustBeNull) {
			Assertions.assertNull(home1.getLinkId());
			Assertions.assertNull(home1.getCoord());
			Assertions.assertNull(work.getLinkId());
			Assertions.assertNull(work.getCoord());
			Assertions.assertNull(home2.getLinkId());
			Assertions.assertNull(home2.getCoord());
		}
	}

	/**
	 * Creates a simple scenario with a network with 3 links and
	 * 10 agents.
	 *
	 * <pre>         (C)
	 *               / \
	 *            8 /   \
	 *             /     \
	 *            /       \
	 *           /      6  \  7
	 *          /           \
	 *         / 9           \
	 *        /               \  5
	 *       /                 \
	 *    0 /                 4 \
	 *     /    1        3       \
	 *    (A)-------------------(B)
	 *                  2
	 * </pre>
	 *
	 * @author mrieser / Senozon AG
	 */
	private static class Fixture {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			createNetwork();
			createPopulation();
		}

		private void createNetwork() {
			Network network = this.scenario.getNetwork();
			NetworkFactory factory = network.getFactory();

			Node a = factory.createNode(Id.create("A", Node.class), new Coord((double) 0, (double) 0));
			Node b = factory.createNode(Id.create("B", Node.class), new Coord((double) 1000, (double) 0));
			Node c = factory.createNode(Id.create("C", Node.class), new Coord((double) 500, (double) 600));

			network.addNode(a);
			network.addNode(b);
			network.addNode(c);

			Link ab = factory.createLink(Id.create("ab", Link.class), a, b);
			Link bc = factory.createLink(Id.create("bc", Link.class), b, c);
			Link ca = factory.createLink(Id.create("ca", Link.class), c, a);

			Set<String> modes = new HashSet<>();
			modes.add("car");
			for (Link l : new Link[] {ab, bc, ca}) {
				l.setLength(1000);
				l.setCapacity(1800);
				l.setNumberOfLanes(1);
				l.setFreespeed(10.0);
				l.setAllowedModes(modes);

				network.addLink(l);
			}
		}

		private void createPopulation() {
			Population pop = scenario.getPopulation();
			PopulationFactory factory = pop.getFactory();

			pop.addPerson(createPersonWithPlan(factory, "1", 200, 10, 800, 300));
			pop.addPerson(createPersonWithPlan(factory, "2", 500, -10, 800, 300));
			pop.addPerson(createPersonWithPlan(factory, "3", 600, 20, 800, 300));
			pop.addPerson(createPersonWithPlan(factory, "4", 900, 100, 400, 500));
			pop.addPerson(createPersonWithPlan(factory, "5", 1000, 300, 400, 500));
			pop.addPerson(createPersonWithPlan(factory, "6", 700, 300, 400, 500));
			pop.addPerson(createPersonWithPlan(factory, "7", 800, 400, 400, 500));
			pop.addPerson(createPersonWithPlan(factory, "8", 440, 500, 300, 100));
			pop.addPerson(createPersonWithPlan(factory, "9", 250, 250, 300, 100));
			pop.addPerson(createPersonWithPlan(factory, "0", 0, 100, 300, 100));
		}

		private Person createPersonWithPlan(PopulationFactory factory, String id, double homeX, double homeY, double workX, double workY) {
			Person person = factory.createPerson(Id.create(id, Person.class));
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			Activity home1 = factory.createActivityFromCoord("home", new Coord(homeX, homeY));
			home1.setEndTime(8*3600);
			Activity work = factory.createActivityFromCoord("work", new Coord(workX, workY));
			work.setEndTime(17*3600);
			Activity home2 = factory.createActivityFromCoord("home", new Coord(homeX, homeY));

			plan.addActivity(home1);
			plan.addLeg(factory.createLeg("car"));
			plan.addActivity(work);
			plan.addLeg(factory.createLeg("car"));
			plan.addActivity(home2);

			return person;
		}

	}
}
