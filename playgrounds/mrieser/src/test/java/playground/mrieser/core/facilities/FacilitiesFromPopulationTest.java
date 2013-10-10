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

package playground.mrieser.core.facilities;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
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
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser / Senozon AG
 */
public class FacilitiesFromPopulationTest {

	@Test
	public void testRun_onePerLink_assignLinks() {
		Fixture f = new Fixture();

		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(true);
		generator.setAssignLinksToFacilitiesIfMissing(true, (NetworkImpl) f.scenario.getNetwork());
		generator.run(f.scenario.getPopulation());
		
		Assert.assertEquals(3, f.scenario.getActivityFacilities().getFacilities().size());
		
		Assert.assertEquals("bc", f.scenario.getActivityFacilities().getFacilities().get(f.scenario.createId("bc")).getLinkId().toString());
		Assert.assertEquals("ca", f.scenario.getActivityFacilities().getFacilities().get(f.scenario.createId("ca")).getLinkId().toString());
		Assert.assertEquals("ab", f.scenario.getActivityFacilities().getFacilities().get(f.scenario.createId("ab")).getLinkId().toString());
		
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("1")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("2")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("3")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("4")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("5")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("6")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("7")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("8")).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("9")).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("0")).getSelectedPlan(), "ca", "ab", true);
	}

	@Test
	public void testRun_onePerLink_assignLinks_openingTimes() {
		Fixture f = new Fixture();
		
		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(true);
		generator.setAssignLinksToFacilitiesIfMissing(true, (NetworkImpl) f.scenario.getNetwork());
		PlanCalcScoreConfigGroup config = new PlanCalcScoreConfigGroup();
		ActivityParams homeParams = new ActivityParams("home");
		ActivityParams workParams = new ActivityParams("work");
		workParams.setOpeningTime(7*3600);
		workParams.setClosingTime(19*3600);
		config.addActivityParams(homeParams);
		config.addActivityParams(workParams);
		generator.assignOpeningTimes(true, config);
		generator.run(f.scenario.getPopulation());
		
		Assert.assertEquals(3, f.scenario.getActivityFacilities().getFacilities().size());
		
		Map<Id, ? extends ActivityFacility> ffs = f.scenario.getActivityFacilities().getFacilities();
		Assert.assertEquals(7*3600, ffs.get(f.scenario.createId("ab")).getActivityOptions().get("work").getOpeningTimes());
		Assert.assertEquals(19*3600, ffs.get(f.scenario.createId("ab")).getActivityOptions().get("work").getOpeningTimes());
		
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("1")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("2")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("3")).getSelectedPlan(), "ab", "bc", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("4")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("5")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("6")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("7")).getSelectedPlan(), "bc", "ca", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("8")).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("9")).getSelectedPlan(), "ca", "ab", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("0")).getSelectedPlan(), "ca", "ab", true);
	}

	@Test
	public void testRun_multiple_assignLinks() {
		Fixture f = new Fixture();
		
		FacilitiesFromPopulation generator = new FacilitiesFromPopulation(f.scenario.getActivityFacilities());
		generator.setOneFacilityPerLink(false);
		generator.setAssignLinksToFacilitiesIfMissing(true, (NetworkImpl) f.scenario.getNetwork());
		generator.run(f.scenario.getPopulation());
		
//		for (ActivityFacility af : f.scenario.getActivityFacilities().getFacilities().values()) {
//			System.out.println(af.getId() + "\t" + af.getLinkId() + "\t" + af.getCoord().getX() + "\t" + af.getCoord().getY());
//		}
		
		Assert.assertEquals(13, f.scenario.getActivityFacilities().getFacilities().size());

		Map<Id, ? extends ActivityFacility> ffs = f.scenario.getActivityFacilities().getFacilities();
		Assert.assertEquals("ab", ffs.get(f.scenario.createId("0")).getLinkId().toString()); // home of agent 1
		Assert.assertEquals("bc", ffs.get(f.scenario.createId("1")).getLinkId().toString()); // work of agent 1-3
		Assert.assertEquals("ab", ffs.get(f.scenario.createId("2")).getLinkId().toString()); // home of agent 2
		Assert.assertEquals("ab", ffs.get(f.scenario.createId("3")).getLinkId().toString()); // home of agent 3
		Assert.assertEquals("bc", ffs.get(f.scenario.createId("4")).getLinkId().toString()); // home of agent 4
		Assert.assertEquals("ca", ffs.get(f.scenario.createId("5")).getLinkId().toString()); // work of agent 4-7
		Assert.assertEquals("bc", ffs.get(f.scenario.createId("6")).getLinkId().toString()); // home of agent 5
		Assert.assertEquals("bc", ffs.get(f.scenario.createId("7")).getLinkId().toString()); // home of agent 6
		Assert.assertEquals("bc", ffs.get(f.scenario.createId("8")).getLinkId().toString()); // home of agent 7
		Assert.assertEquals("ca", ffs.get(f.scenario.createId("9")).getLinkId().toString()); // home of agent 8
		Assert.assertEquals("ab", ffs.get(f.scenario.createId("10")).getLinkId().toString()); // work of agent 8-10
		Assert.assertEquals("ca", ffs.get(f.scenario.createId("11")).getLinkId().toString()); // home of agent 9
		Assert.assertEquals("ca", ffs.get(f.scenario.createId("12")).getLinkId().toString()); // home of agent 10
		
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("1")).getSelectedPlan(), "0", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("2")).getSelectedPlan(), "2", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("3")).getSelectedPlan(), "3", "1", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("4")).getSelectedPlan(), "4", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("5")).getSelectedPlan(), "6", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("6")).getSelectedPlan(), "7", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("7")).getSelectedPlan(), "8", "5", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("8")).getSelectedPlan(), "9", "10", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("9")).getSelectedPlan(), "11", "10", true);
		assertPlan(f.scenario.getPopulation().getPersons().get(f.scenario.createId("0")).getSelectedPlan(), "12", "10", true);
	}
	
	private void assertPlan(Plan plan, String homeFacilityId, String workFacilityId, boolean linkCoordMustBeNull) {
		Activity home1 = (Activity) plan.getPlanElements().get(0);
		Activity work = (Activity) plan.getPlanElements().get(2);
		Activity home2 = (Activity) plan.getPlanElements().get(4);
		
		Assert.assertEquals(homeFacilityId, home1.getFacilityId().toString());
		Assert.assertEquals(workFacilityId, work.getFacilityId().toString());
		Assert.assertEquals(homeFacilityId, home2.getFacilityId().toString());
		
		if (linkCoordMustBeNull) {
			Assert.assertNull(home1.getLinkId());
			Assert.assertNull(home1.getCoord());
			Assert.assertNull(work.getLinkId());
			Assert.assertNull(work.getCoord());
			Assert.assertNull(home2.getLinkId());
			Assert.assertNull(home2.getCoord());
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
			
			Node a = factory.createNode(this.scenario.createId("A"), this.scenario.createCoord(0, 0));
			Node b = factory.createNode(this.scenario.createId("B"), this.scenario.createCoord(1000, 0));
			Node c = factory.createNode(this.scenario.createId("C"), this.scenario.createCoord(500, 600));
			
			network.addNode(a);
			network.addNode(b);
			network.addNode(c);
			
			Link ab = factory.createLink(this.scenario.createId("ab"), a, b);
			Link bc = factory.createLink(this.scenario.createId("bc"), b, c);
			Link ca = factory.createLink(this.scenario.createId("ca"), c, a);
			
			Set<String> modes = new HashSet<String>();
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
			Person person = factory.createPerson(scenario.createId(id));
			Plan plan = factory.createPlan();
			person.addPlan(plan);
			Activity home1 = factory.createActivityFromCoord("home", scenario.createCoord(homeX, homeY));
			home1.setEndTime(8*3600);
			Activity work = factory.createActivityFromCoord("work", scenario.createCoord(workX, workY));
			work.setEndTime(17*3600);
			Activity home2 = factory.createActivityFromCoord("home", scenario.createCoord(homeX, homeY));
			
			plan.addActivity(home1);
			plan.addLeg(factory.createLeg("car"));
			plan.addActivity(work);
			plan.addLeg(factory.createLeg("car"));
			plan.addActivity(home2);
			
			return person;
		}
		
	}
}
