/* *********************************************************************** *
 * project: org.matsim.*
 * InsertParkingActivitiesTest.java
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

package playground.wrashid.parkingSearch.withinday;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.agents.ExperimentalBasicWithindayAgent;

public class InsertParkingActivitiesTest extends TestCase {

	private static final Logger log = Logger.getLogger(InsertParkingActivitiesTest.class);
	
	public void testInsertParkingActivities() {
		Config config = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		createFacilities(sc);
		
		PopulationFactory factory = sc.getPopulation().getFactory();
		
		Person person = factory.createPerson(sc.createId("1"));
		sc.getPopulation().addPerson(person);
		Plan plan = factory.createPlan();
		person.addPlan(plan);
		
		ActivityImpl activity;
		
		activity = (ActivityImpl) factory.createActivityFromLinkId("home", sc.createId("l2"));
		activity.setFacilityId(sc.createId("f2"));
		plan.addActivity(activity);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		activity = (ActivityImpl) factory.createActivityFromLinkId("work", sc.createId("l4"));
		activity.setFacilityId(sc.createId("f4"));
		plan.addActivity(activity);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		activity = (ActivityImpl) factory.createActivityFromLinkId("home", sc.createId("l2"));
		activity.setFacilityId(sc.createId("f2"));
		plan.addActivity(activity);
		
		plan.addLeg(factory.createLeg(TransportMode.walk));
		
		activity = (ActivityImpl) factory.createActivityFromLinkId("shopping", sc.createId("l5"));
		activity.setFacilityId(sc.createId("f5"));
		plan.addActivity(activity);
		
		plan.addLeg(factory.createLeg(TransportMode.pt));
		
		activity = (ActivityImpl) factory.createActivityFromLinkId("home", sc.createId("l2"));
		activity.setFacilityId(sc.createId("f2"));
		plan.addActivity(activity);
		
		/*
		 * Set activity durations and coordinates
		 */
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof ActivityImpl) {
				activity = (ActivityImpl) planElement;
				activity.setMaximumDuration(3600);
				activity.setCoord(sc.getNetwork().getLinks().get(activity.getLinkId()).getCoord());
			}
		}
		
		
		assertEquals(9, plan.getPlanElements().size());
		
		TravelTimeCalculatorFactory ttCalcFactory = new TravelTimeCalculatorFactoryImpl();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) sc.getPopulation().getFactory()).getModeRouteFactory();
		PersonalizableTravelTime travelTime = ttCalcFactory.createTravelTimeCalculator(sc.getNetwork(), sc.getConfig().travelTimeCalculator()) ;
		PersonalizableTravelCost travelCost = new TravelTimeDistanceCostCalculator(travelTime, sc.getConfig().planCalcScore());
		
		PlanAlgorithm plansAlgorithm = new PlansCalcRoute(config.plansCalcRoute(), sc.getNetwork(), travelCost, travelTime, routeFactory);
		
		// initialize routes
		new PersonPrepareForSim(plansAlgorithm, (ScenarioImpl) sc).run(sc.getPopulation());

		ParkingInfrastructure parkingInfrastructure = new ParkingInfrastructure(sc);
		InsertParkingActivities insertParkingActivities = new InsertParkingActivities(sc, plansAlgorithm, parkingInfrastructure);
		
		ExperimentalBasicWithindayAgent agent = ExperimentalBasicWithindayAgent
				.createExperimentalBasicWithindayAgent(person, null);
		insertParkingActivities.run(agent.getSelectedPlan());
		
		assertEquals(17, agent.getSelectedPlan().getPlanElements().size());
		
		for (PlanElement planElement : agent.getSelectedPlan().getPlanElements()) {
			log.info(planElement.toString());
		}
				
		/*
		 * Add two consecutive car legs which cannot be handled by the Replanner
		 */
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addActivity(factory.createActivityFromLinkId("leisure", sc.createId("l6")));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addActivity(factory.createActivityFromLinkId("home", sc.createId("l2")));
		
		agent = ExperimentalBasicWithindayAgent
				.createExperimentalBasicWithindayAgent(person, null);
		try {
			insertParkingActivities.run(agent.getSelectedPlan());
			Assert.fail("Expected RuntimeException, but there was none.");
		} catch (RuntimeException e) { }
	}
	
	/*
	 * Network:
	 *           l7
	 * ------------------------
	 * |                      |
	 * |  ---------------5    |
	 * |  |      l8      |    |
	 * |  |              |l4  |
	 * |  | l1   l2   l3 | l5 |
	 * ---1----2----3----4----6
	 *    |              |
	 *    |              |l6
	 *    |      l9      |
	 *    ---------------7
	 */
	private void createNetwork(Scenario sc) {
		
		Network network = sc.getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		
		Node n1 = networkFactory.createNode(sc.createId("n1"), sc.createCoord(0, 0));
		Node n2 = networkFactory.createNode(sc.createId("n2"), sc.createCoord(10000, 0));
		Node n3 = networkFactory.createNode(sc.createId("n3"), sc.createCoord(20000, 0));
		Node n4 = networkFactory.createNode(sc.createId("n4"), sc.createCoord(30000, 0));
		Node n5 = networkFactory.createNode(sc.createId("n5"), sc.createCoord(30000, 10000));
		Node n6 = networkFactory.createNode(sc.createId("n6"), sc.createCoord(40000, 0));
		Node n7 = networkFactory.createNode(sc.createId("n7"), sc.createCoord(30000, -10000));
		
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		network.addNode(n5);
		network.addNode(n6);
		network.addNode(n7);
		
		Link l1 = networkFactory.createLink(sc.createId("l1"), n1, n2);
		Link l2 = networkFactory.createLink(sc.createId("l2"), n2, n3);
		Link l3 = networkFactory.createLink(sc.createId("l3"), n3, n4);
		Link l4 = networkFactory.createLink(sc.createId("l4"), n4, n5);
		Link l5 = networkFactory.createLink(sc.createId("l5"), n4, n6);
		Link l6 = networkFactory.createLink(sc.createId("l6"), n5, n7);
		Link l7 = networkFactory.createLink(sc.createId("l7"), n5, n1);
		Link l8 = networkFactory.createLink(sc.createId("l8"), n6, n1);
		Link l9 = networkFactory.createLink(sc.createId("l9"), n7, n1);
		
		l1.setFreespeed(10.0);
		l2.setFreespeed(10.0);
		l3.setFreespeed(10.0);
		l4.setFreespeed(10.0);
		l5.setFreespeed(10.0);
		l6.setFreespeed(10.0);
		l7.setFreespeed(10.0);
		l8.setFreespeed(10.0);
		l9.setFreespeed(10.0);
		
		l1.setLength(10000.0);
		l2.setLength(10000.0);
		l3.setLength(10000.0);
		l4.setLength(10000.0);
		l5.setLength(10000.0);
		l6.setLength(10000.0);
		l7.setLength(40000.0);
		l8.setLength(40000.0);
		l9.setLength(40000.0);
		
		network.addLink(l1);
		network.addLink(l2);
		network.addLink(l3);
		network.addLink(l4);
		network.addLink(l5);
		network.addLink(l6);
		network.addLink(l7);
		network.addLink(l8);
		network.addLink(l9);
	}
	
	private void createFacilities(Scenario sc) {
		
		ActivityFacilityImpl facility = null;
		ActivityFacilitiesImpl facilities = ((ScenarioImpl) sc).getActivityFacilities();
		
		facility = facilities.createFacility(sc.createId("f1"), sc.getNetwork().getLinks().get(sc.createId("l1")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createFacility(sc.createId("f2"), sc.getNetwork().getLinks().get(sc.createId("l2")).getCoord());
		facility.createActivityOption("home");
		
		facility = facilities.createFacility(sc.createId("f3"), sc.getNetwork().getLinks().get(sc.createId("l3")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createFacility(sc.createId("f4"), sc.getNetwork().getLinks().get(sc.createId("l4")).getCoord());
		facility.createActivityOption("work");
		
		facility = facilities.createFacility(sc.createId("f5"), sc.getNetwork().getLinks().get(sc.createId("l5")).getCoord());
		facility.createActivityOption("shopping");
		
		facility = facilities.createFacility(sc.createId("f6"), sc.getNetwork().getLinks().get(sc.createId("l6")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createFacility(sc.createId("f7"), sc.getNetwork().getLinks().get(sc.createId("l7")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createFacility(sc.createId("f8"), sc.getNetwork().getLinks().get(sc.createId("l8")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createFacility(sc.createId("f9"), sc.getNetwork().getLinks().get(sc.createId("l9")).getCoord());
		facility.createActivityOption("parking");		
	}

}
