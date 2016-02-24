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

import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.withindayFW.core.InsertParkingActivities;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.core.mobsim.ParkingPopulationAgentSource;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingCostCalculatorFW;

public class InsertParkingActivitiesTest extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(InsertParkingActivitiesTest.class);

	public void testInsertParkingActivities() {
		Config config = super.loadConfig(null);
		Scenario sc = ScenarioUtils.createScenario(config);
		createNetwork(sc);
		createFacilities(sc);

		PopulationFactory factory = sc.getPopulation().getFactory();

		Person person = factory.createPerson(Id.create("1", Person.class));
		sc.getPopulation().addPerson(person);
		Plan plan = factory.createPlan();
		person.addPlan(plan);

		ActivityImpl activity;

		activity = (ActivityImpl) factory.createActivityFromLinkId("home", Id.create("l2", Link.class));
		activity.setFacilityId(Id.create("f2", ActivityFacility.class));
		plan.addActivity(activity);

		plan.addLeg(factory.createLeg(TransportMode.car));

		activity = (ActivityImpl) factory.createActivityFromLinkId("work", Id.create("l4", Link.class));
		activity.setFacilityId(Id.create("f4", ActivityFacility.class));
		plan.addActivity(activity);

		plan.addLeg(factory.createLeg(TransportMode.car));

		activity = (ActivityImpl) factory.createActivityFromLinkId("home", Id.create("l2", Link.class));
		activity.setFacilityId(Id.create("f2", ActivityFacility.class));
		plan.addActivity(activity);

		plan.addLeg(factory.createLeg(TransportMode.walk));

		activity = (ActivityImpl) factory.createActivityFromLinkId("shopping", Id.create("l5", Link.class));
		activity.setFacilityId(Id.create("f5", ActivityFacility.class));
		plan.addActivity(activity);

		plan.addLeg(factory.createLeg(TransportMode.pt));

		activity = (ActivityImpl) factory.createActivityFromLinkId("home", Id.create("l2", Link.class));
		activity.setFacilityId(Id.create("f2", ActivityFacility.class));
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

		TravelTime travelTime = TravelTimeCalculator.create(sc.getNetwork(), sc.getConfig().travelTimeCalculator()).getLinkTravelTimes() ;

		TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutility.Builder( TransportMode.car, config.planCalcScore() ).createTravelDisutility(travelTime ) ;
		
		TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults() ;
		builder.setLeastCostPathCalculatorFactory( new DijkstraFactory() );
		builder.setTravelTime(travelTime);
		builder.setTravelDisutility(travelDisutility);
		
		TripRouter tripRouter = builder.build( sc ).get() ;

		// initialize routes
		new PersonPrepareForSim(new PlanRouter(tripRouter), sc).run(sc.getPopulation());

		//		ParkingInfrastructure parkingInfrastructure = new ParkingInfrastructure(sc, null, null);
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();
		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);

		for (ActivityFacility facility : ((MutableScenario) sc).getActivityFacilities().getFacilities().values()) {
			// if the facility offers a parking activity
			if (facility.getActivityOptions().containsKey("parking")) {
				if (MatsimRandom.getRandom().nextBoolean()){
					streetParking.add(facility.getId());
				} else {
					garageParking.add(facility.getId());
				}
			}
		}

		ParkingInfrastructure parkingInfrastructure = new ParkingInfrastructure(sc, parkingTypes, new ParkingCostCalculatorFW(parkingTypes));

		InsertParkingActivities insertParkingActivities = new InsertParkingActivities(sc, tripRouter, parkingInfrastructure);

		// init parking facility capacities
		IntegerValueHashMap<Id> facilityCapacities = new IntegerValueHashMap<Id>();
		parkingInfrastructure.setFacilityCapacities(facilityCapacities);
		for (ActivityFacility parkingFacility : parkingInfrastructure.getParkingFacilities()) {
			facilityCapacities.incrementBy(parkingFacility.getId(), 10);
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		QSim qSim = new QSim(sc, eventsManager);
		QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		//        AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim) ;
		AgentSource agentSource = new ParkingPopulationAgentSource(sc.getPopulation(), agentFactory, qSim, 
				insertParkingActivities, parkingInfrastructure, 1);
		qSim.addAgentSource(agentSource);

		agentSource.insertAgentsIntoMobsim(); 
		PlanAgent agent = null;
		for (MobsimAgent a : qSim.getAgents()) {
			agent = (PlanAgent) a;
			break;
		}

		//		ExperimentalBasicWithindayAgent agent = ExperimentalBasicWithindayAgent.createExperimentalBasicWithindayAgent(person, qSim);
		//		insertParkingActivities.run(agent.getSelectedPlan());

		assertEquals(17, agent.getCurrentPlan().getPlanElements().size());

		for (PlanElement planElement : agent.getCurrentPlan().getPlanElements()) {
			log.info(planElement.toString());
		}

		/*
		 * Add two consecutive car legs which cannot be handled by the Replanner
		 */
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addActivity(factory.createActivityFromLinkId("leisure", Id.create("l6", Link.class)));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addLeg(factory.createLeg(TransportMode.car));
		plan.addActivity(factory.createActivityFromLinkId("home", Id.create("l2", Link.class)));

		//		agent = ExperimentalBasicWithindayAgent.createExperimentalBasicWithindayAgent(person, qSim);
		agent = new PersonDriverAgentImpl(person.getSelectedPlan(),qSim) ;
		try {
			insertParkingActivities.run(agent.getCurrentPlan());
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

	private void createFacilities(Scenario sc) {

		ActivityFacility facility = null;
		ActivityFacilities facilities = sc.getActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();

		facility = factory.createActivityFacility(Id.create("f1", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l1", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));

		facility = factory.createActivityFacility(Id.create("f2", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l2", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("home"));

		facility = factory.createActivityFacility(Id.create("f3", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l3", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));

		facility = factory.createActivityFacility(Id.create("f4", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l4", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("work"));

		facility = factory.createActivityFacility(Id.create("f5", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l5", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("shopping"));

		facility = factory.createActivityFacility(Id.create("f6", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l6", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));

		facility = factory.createActivityFacility(Id.create("f7", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l7", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));

		facility = factory.createActivityFacility(Id.create("f8", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l8", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));

		facility = factory.createActivityFacility(Id.create("f9", ActivityFacility.class), sc.getNetwork().getLinks().get(Id.create("l9", Link.class)).getCoord());
		facilities.addActivityFacility(facility);
		facility.addActivityOption(factory.createActivityOption("parking"));		
	}

}
