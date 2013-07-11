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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgentFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryImpl;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.lib.obj.IntegerValueHashMap;
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
		TravelTime travelTime = ttCalcFactory.createTravelTimeCalculator(sc.getNetwork(), sc.getConfig().travelTimeCalculator()).getLinkTravelTimes() ;
		
		TripRouter tripRouter = new TripRouterFactoryImpl(sc, new TravelCostCalculatorFactoryImpl(), travelTime, new DijkstraFactory(), null).instantiateAndConfigureTripRouter();
		
		// initialize routes
		new PersonPrepareForSim(new PlanRouter(tripRouter), (ScenarioImpl) sc).run(sc.getPopulation());

//		ParkingInfrastructure parkingInfrastructure = new ParkingInfrastructure(sc, null, null);
		HashMap<String, HashSet<Id>> parkingTypes = new HashMap<String, HashSet<Id>>();
		HashSet<Id> streetParking = new HashSet<Id>();
		HashSet<Id> garageParking = new HashSet<Id>();
		parkingTypes.put("streetParking", streetParking);
		parkingTypes.put("garageParking", garageParking);
		
		for (ActivityFacility facility : ((ScenarioImpl) sc).getActivityFacilities().getFacilities().values()) {
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

		sc.getConfig().addQSimConfigGroup(new QSimConfigGroup());
		EventsManager eventsManager = EventsUtils.createEventsManager();
		QSim qSim = new QSim(sc, eventsManager);
		QNetsimEngineFactory netsimEngFactory = new DefaultQSimEngineFactory();
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
        AgentFactory agentFactory = new ExperimentalBasicWithindayAgentFactory(qSim);
        AgentSource agentSource = new ParkingPopulationAgentSource(sc.getPopulation(), agentFactory, qSim, 
        		insertParkingActivities, parkingInfrastructure, 1);
        qSim.addAgentSource(agentSource);
		
        agentSource.insertAgentsIntoMobsim(); 
        ExperimentalBasicWithindayAgent agent = null;
        for (MobsimAgent a : qSim.getAgents()) {
        	agent = (ExperimentalBasicWithindayAgent) a;
        	break;
        }
        
//		ExperimentalBasicWithindayAgent agent = ExperimentalBasicWithindayAgent.createExperimentalBasicWithindayAgent(person, qSim);
//		insertParkingActivities.run(agent.getSelectedPlan());
		
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
		
		agent = ExperimentalBasicWithindayAgent.createExperimentalBasicWithindayAgent(person, qSim);
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
		
		facility = facilities.createAndAddFacility(sc.createId("f1"), sc.getNetwork().getLinks().get(sc.createId("l1")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createAndAddFacility(sc.createId("f2"), sc.getNetwork().getLinks().get(sc.createId("l2")).getCoord());
		facility.createActivityOption("home");
		
		facility = facilities.createAndAddFacility(sc.createId("f3"), sc.getNetwork().getLinks().get(sc.createId("l3")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createAndAddFacility(sc.createId("f4"), sc.getNetwork().getLinks().get(sc.createId("l4")).getCoord());
		facility.createActivityOption("work");
		
		facility = facilities.createAndAddFacility(sc.createId("f5"), sc.getNetwork().getLinks().get(sc.createId("l5")).getCoord());
		facility.createActivityOption("shopping");
		
		facility = facilities.createAndAddFacility(sc.createId("f6"), sc.getNetwork().getLinks().get(sc.createId("l6")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createAndAddFacility(sc.createId("f7"), sc.getNetwork().getLinks().get(sc.createId("l7")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createAndAddFacility(sc.createId("f8"), sc.getNetwork().getLinks().get(sc.createId("l8")).getCoord());
		facility.createActivityOption("parking");
		
		facility = facilities.createAndAddFacility(sc.createId("f9"), sc.getNetwork().getLinks().get(sc.createId("l9")).getCoord());
		facility.createActivityOption("parking");		
	}

}
