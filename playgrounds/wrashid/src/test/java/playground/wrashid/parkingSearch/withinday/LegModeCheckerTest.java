/* *********************************************************************** *
 * project: org.matsim.*
 * LegModeCheckerTest.java
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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

public class LegModeCheckerTest extends MatsimTestCase {

	public void testUpdateLegMode() {
		Config config = super.loadConfig(null);
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		
		PopulationFactory factory = sc.getPopulation().getFactory();
		ActivityFacilities facilities = sc.getActivityFacilities();
		
		Person person = factory.createPerson(Id.create("1", Person.class));
		Plan plan = factory.createPlan();
		person.addPlan(plan);
		
		String[] modes = {TransportMode.car, TransportMode.walk, TransportMode.car};
		plan.addActivity(factory.createActivityFromLinkId("home", Id.create("l2", Link.class)));
		plan.addLeg(factory.createLeg(modes[0]));
		plan.addActivity(factory.createActivityFromLinkId("work", Id.create("l4", Link.class)));
		plan.addLeg(factory.createLeg(modes[1]));
		plan.addActivity(factory.createActivityFromLinkId("shopping", Id.create("l5", Link.class)));
		plan.addLeg(factory.createLeg(modes[2]));
		plan.addActivity(factory.createActivityFromLinkId("home", Id.create("l2", Link.class)));
		
		/*
		 * Set activity durations and coordinates and create facilities
		 */
		ActivityFacilitiesFactory ffactory = facilities.getFactory();
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) planElement;
				activity.setMaximumDuration(3600);
				activity.setCoord(sc.getNetwork().getLinks().get(activity.getLinkId()).getCoord());
				activity.setFacilityId(Id.create(activity.getLinkId(), ActivityFacility.class));
				
				ActivityFacility facility = facilities.getFacilities().get(activity.getLinkId());
				if (facility == null) {
					facility = ffactory.createActivityFacility(Id.create(activity.getLinkId(), ActivityFacility.class), activity.getCoord());
					facilities.addActivityFacility(facility);
				}
				ActivityOption activityOption = facility.getActivityOptions().get(activity.getType());
				if (activityOption == null) {
					activityOption = ffactory.createActivityOption(activity.getType());
					facility.addActivityOption(activityOption);
				}
			}
		}
		
		/*
		 * Create PlansCalcRoute object to reroute legs with adapted mode 
		 */
		TravelTime travelTimes = new FreeSpeedTravelTime();
		TravelDisutility travelCosts = new Builder( TransportMode.car, config.planCalcScore() ).createTravelDisutility(travelTimes);
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
		builder.setLeastCostPathCalculatorFactory( new DijkstraFactory() );
		builder.setTravelTime(travelTimes);
		builder.setTravelDisutility(travelCosts);
		PlanAlgorithm plansCalcRoute = new PlanRouter( builder.build( sc ).get() ) ;

		/*
		 * Create LegModeChecker to check and adapt leg modes
		 */
		LegModeChecker legModeChecker = new LegModeChecker(sc, plansCalcRoute);
		legModeChecker.setValidNonCarModes(new String[]{TransportMode.walk});
		
		/*
		 * Always change to car
		 * Expect switch from car-walk-car to car-car-car 
		 */
		legModeChecker.setToCarProbability(1.0);
		legModeChecker.run(plan);
		assertEquals(modes[0], ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals(TransportMode.car, ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals(modes[2], ((Leg) plan.getPlanElements().get(5)).getMode());
		
		/*
		 * reset modes
		 */
		((Leg) plan.getPlanElements().get(1)).setMode(modes[0]);
		((Leg) plan.getPlanElements().get(3)).setMode(modes[1]);
		((Leg) plan.getPlanElements().get(5)).setMode(modes[2]);
		
		/*
		 * Always change to non-car
		 * Expect switch from car-walk-car to car-walk-walk
		 */
		legModeChecker.setToCarProbability(0.0);
		legModeChecker.run(plan);
		assertEquals(modes[0], ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals(modes[1], ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals(TransportMode.walk, ((Leg) plan.getPlanElements().get(5)).getMode());
		
		/*
		 * reset modes
		 */
		((Leg) plan.getPlanElements().get(1)).setMode(modes[0]);
		((Leg) plan.getPlanElements().get(3)).setMode(modes[1]);
		((Leg) plan.getPlanElements().get(5)).setMode(modes[2]);
		
		/*
		 * Increase max distance between an activity and the parked car
		 * Expect no mode switch
		 */
		legModeChecker.setToCarProbability(0.5);
		legModeChecker.setMaxDistance(Double.MAX_VALUE);
		legModeChecker.run(plan);
		assertEquals(modes[0], ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals(modes[1], ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals(modes[2], ((Leg) plan.getPlanElements().get(5)).getMode());
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

		Node n1 = networkFactory.createNode(Id.create("n1", Node.class), new Coord((double) 0, (double) 0));
		Node n2 = networkFactory.createNode(Id.create("n2", Node.class), new Coord((double) 10000, (double) 0));
		Node n3 = networkFactory.createNode(Id.create("n3", Node.class), new Coord((double) 20000, (double) 0));
		Node n4 = networkFactory.createNode(Id.create("n4", Node.class), new Coord((double) 30000, (double) 0));
		Node n5 = networkFactory.createNode(Id.create("n5", Node.class), new Coord((double) 30000, (double) 10000));
		Node n6 = networkFactory.createNode(Id.create("n6", Node.class), new Coord((double) 40000, (double) 0));
		double y = -10000;
		Node n7 = networkFactory.createNode(Id.create("n7", Node.class), new Coord((double) 30000, y));
		
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		network.addNode(n5);
		network.addNode(n6);
		network.addNode(n7);
		
		Link l1 = networkFactory.createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = networkFactory.createLink(Id.create("l2", Link.class), n2, n3);
		Link l3 = networkFactory.createLink(Id.create("l3", Link.class), n3, n4);
		Link l4 = networkFactory.createLink(Id.create("l4", Link.class), n4, n5);
		Link l5 = networkFactory.createLink(Id.create("l5", Link.class), n4, n6);
		Link l6 = networkFactory.createLink(Id.create("l6", Link.class), n5, n7);
		Link l7 = networkFactory.createLink(Id.create("l7", Link.class), n5, n1);
		Link l8 = networkFactory.createLink(Id.create("l8", Link.class), n6, n1);
		Link l9 = networkFactory.createLink(Id.create("l9", Link.class), n7, n1);
		
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

}
