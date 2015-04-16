/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerQNetsimEngineTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * 
 * @author cdobler
 */
public class PassengerQNetworkEngineTest extends MatsimTestCase {

	private static Logger log = Logger.getLogger(PassengerQNetworkEngineTest.class);
	
	/*
	 * Departure is scheduled at another link than defined in the agent's plan
	 */
	public void testInvalidDepartureWrongLink() {
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);

		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture, jointDepartureOrganizer);				
			}
		});
		
		boolean caughtException = false;
		try {
			qSim.run();			
		} catch (RuntimeException e) {
			log.info("Caught expected runtime exception.");
			caughtException = true;
		}
		assertEquals(true, caughtException);
	}
	
	/*
	 * Departure is scheduled at another link than defined in the agent's plan
	 */
	public void testTooManyPassengers() {
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger1 = makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger2 = makeNonStopPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger3 = makeNonStopPassenger(Id.create("p4", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger4 = makeNonStopPassenger(Id.create("p5", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger5 = makeNonStopPassenger(Id.create("p6", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				passengerIds.add(Id.create("p3", Person.class));
				passengerIds.add(Id.create("p4", Person.class));
				passengerIds.add(Id.create("p5", Person.class));
				passengerIds.add(Id.create("p6", Person.class));
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger1, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger2, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger3, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger4, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger5, 1, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		
		boolean caughtException = false;
		try {
			qSim.run();			
		} catch (RuntimeException e) {
			log.info("Caught expected runtime exception.");
			caughtException = true;
		}
		assertEquals(true, caughtException);
	}
	
	/*
	 * Only driver from l1 to l3
	 */
	public void testOnlyDriver() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(1, eventsCounter.getEnterCount());
		assertEquals(1, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger from l1 to l3
	 * Driver ends activity before passenger
	 */
	public void testDriverAndPassenger1() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger = makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3660.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(2, eventsCounter.getEnterCount());
		assertEquals(2, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger from l1 to l3
	 * Driver ends activity after passenger
	 */
	public void testDriverAndPassenger2() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3660.0);
		final Person passenger = makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(2, eventsCounter.getEnterCount());
		assertEquals(2, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger from l2 to l3
	 * Driver ends activity after passenger
	 * Passenger traveled from l1 to l2 with a different vehicle
	 * Tests whether an agent's vehicle is set to null after arrival
	 */
	public void testDriverAndPassenger3() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger = makeDriverThenPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v0", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 3, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(4, eventsCounter.getEnterCount());
		assertEquals(4, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one picked up passenger
	 * Driver reaches pickup location before passenger
	 */
	public void testPickup1() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger = makePickUpPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3700.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}

	/*
	 * Driver and one picked up passenger
	 * Driver reaches pickup location after passenger
	 */
	public void testPickup2() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3700.0);
		final Person passenger = makePickUpPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3650.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver, one passenger and one picked up passenger
	 * Driver reaches pickup location before passenger
	 */
	public void testPickup3() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger1 = makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger2 = makePickUpPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3700.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger1, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				passengerIds.add(Id.create("p3", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger2, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p3")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(4, eventsCounter.getEnterCount());
		assertEquals(4, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p3", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger which is picked up and dropped off on the same link.
	 * Passenger waits before vehicles arrives.
	 */
	public void testPickUpDropOff1() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makePickupDropOffDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger = makePickUpDropOffPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 5, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		/*
		 * Enter- and leave vehicle events are only created for car leg that do not 
		 * end immediately on the same link as they have started.
		 * Passengers ALWAYS create those events!
		 */
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger which is picked up and dropped off on the same link.
	 * Passenger arrives after vehicle.
	 */
	public void testPickUpDropOff2() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makePickupDropOffDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger = makePickUpDropOffPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3700.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 5, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		/*
		 * Enter- and leave vehicle events are only created for car leg that do not 
		 * end immediately on the same link as they have started.
		 * Passengers ALWAYS create those events!
		 */
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver and one passenger which is dropped off
	 */
	public void testDropOff() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3650.0);
		final Person passenger = makeDropOffPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver starts with three passengers on link l1.
	 * One of them leaves at link l2.
	 * Another one enters at link l2.
	 */
	public void testOverall() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driver = makeDropOffPickupDriver(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0, 3700.0);
		final Person passenger1 = makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger2 = makeNonStopPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger3 = makeDropOffPassenger(Id.create("p4", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger4 = makePickUpPassenger(Id.create("p5", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3780.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));
		
		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				passengerIds.add(Id.create("p3", Person.class));
				passengerIds.add(Id.create("p4", Person.class));
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 1, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger1, 1, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger2, 1, jointDeparture1, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger3, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p2", Person.class));
				passengerIds.add(Id.create("p3", Person.class));
				passengerIds.add(Id.create("p5", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driver, 3, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger4, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p3")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p4")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p5")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(6, eventsCounter.getEnterCount());
		assertEquals(6, eventsCounter.getLeaveCount());

		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p3", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p4", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p5", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver A from l1 to l2 and Driver B from l2 to l3.
	 * Driver B wants do depart before Driver A has arrived.
	 */
	public void testDriverSwitch() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driverA = makeDriverA(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person driverB = makeDriverB(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driverA, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p2", Person.class), passengerIds);
				assignJointDeparture(qSim, driverB, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(2, eventsCounter.getEnterCount());
		assertEquals(2, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver A from l1 to l2 and Driver B from l2 to l3.
	 * Driver B wants do depart before Driver A has arrived.
	 * A Passenger from l2 to l3 arrives before Driver A has
	 * arrived.
	 */
	public void testDriverSwitchAndEarlierPickup() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driverA = makeDriverA(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3650.0);
		final Person driverB = makeDriverB(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3650.0);
		final Person passenger = makePickUpPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));
		
		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driverA, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p3", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p2", Person.class), passengerIds);
				assignJointDeparture(qSim, driverB, 1, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p3")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p3", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}

	/*
	 * Driver A from l1 to l2 and Driver B from l2 to l3.
	 * Driver B wants do depart before Driver A has arrived.
	 * A Passenger from l2 to l3 arrives after Driver A has
	 * arrived but before Driver B has arrived.
	 */
	public void testDriverSwitchAndBetweenPickup() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driverA = makeDriverA(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person driverB = makeDriverB(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3700.0);
		final Person passenger = makePickUpPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3650.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driverA, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p3", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p2", Person.class), passengerIds);
				assignJointDeparture(qSim, driverB, 1, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p3")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p3", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Driver A from l1 to l2 and Driver B from l2 to l3.
	 * Driver B wants do depart before Driver A has arrived.
	 * A Passenger from l2 to l3 arrives after Driver A has
	 * arrived.
	 */
	public void testDriverSwitchAndLaterPickup() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driverA = makeDriverA(Id.create("p1", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person driverB = makeDriverB(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person passenger = makePickUpPassenger(Id.create("p3", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3650.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture1 = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driverA, 1, jointDeparture1, jointDepartureOrganizer);
				
				passengerIds = new LinkedHashSet<>();
				passengerIds.add(Id.create("p3", Person.class));
				JointDeparture jointDeparture2 = jointDepartureOrganizer.createJointDeparture(Id.create("jd2", JointDeparture.class), Id.create("1to2", Link.class), Id.create("v1", Vehicle.class), Id.create("p2", Person.class), passengerIds);
				assignJointDeparture(qSim, driverB, 1, jointDeparture2, jointDepartureOrganizer);
				assignJointDeparture(qSim, passenger, 1, jointDeparture2, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("1to2", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p3")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		assertEquals(3, eventsCounter.getEnterCount());
		assertEquals(3, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p3", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * If no joint departure is found, the default departure handler should be used.
	 */
	public void testNonJointDeparture() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		makeNonStopPassenger(Id.create("p2", Person.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		QSim qSim = tuple.getFirst();
		JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}
		}
		
		// check number of enter and leave events
		// the passenger is teleported and therefore does NOT create enter/leave events
		assertEquals(1, eventsCounter.getEnterCount());
		assertEquals(1, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}
	
	/*
	 * Test one agent (p1) with a joint departure and one without (p2).
	 */
	public void testJointAndNonJointDeparture() {
		
		Scenario scenario = makeScenario();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(new EventsPrinter());
		EventsCounter eventsCounter = new EventsCounter();
		eventsManager.addHandler(eventsCounter);
		
		makeNetwork(scenario.getNetwork());
		final Person driverA = makeNonStopDriver(Id.create("p1", Person.class), Id.create("v1", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		final Person driverB = makeNonStopDriver(Id.create("p2", Person.class), Id.create("v2", Vehicle.class), scenario.getPopulation(), scenario.getNetwork(), 3600.0);
		
		Tuple<QSim, JointDepartureOrganizer> tuple = makeQSim(scenario, eventsManager); 
		final QSim qSim = tuple.getFirst();
		final JointDepartureOrganizer jointDepartureOrganizer = tuple.getSecond();
		
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v1", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));
		qSim.createAndParkVehicleOnLink(VehicleUtils.getFactory().createVehicle(Id.create("v2", Vehicle.class), VehicleUtils.getDefaultVehicleType()), Id.create("0to1", Link.class));

		qSim.addQueueSimulationListeners(new MobsimInitializedListener() {
			@Override
			public void notifyMobsimInitialized(MobsimInitializedEvent e) {	
				Set<Id<Person>> passengerIds = new LinkedHashSet<>();
				JointDeparture jointDeparture = jointDepartureOrganizer.createJointDeparture(Id.create("jd1", JointDeparture.class), Id.create("0to1", Link.class), Id.create("v1", Vehicle.class), Id.create("p1", Person.class), passengerIds);
				assignJointDeparture(qSim, driverA, 1, jointDeparture, jointDepartureOrganizer);
			}
		});
		
		qSim.run();
		
		// check agents final destinations
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			if (mobsimAgent.getId().toString().equals("p1")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			} else if (mobsimAgent.getId().toString().equals("p2")) {
				assertEquals(Id.create("2to3", Link.class), mobsimAgent.getCurrentLinkId());
			}

		}
		
		// check number of enter and leave events
		assertEquals(2, eventsCounter.getEnterCount());
		assertEquals(2, eventsCounter.getLeaveCount());
		
		// check whether all scheduled joint departures have been processed
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p1", Person.class)));
		assertEquals(false, peekJointDeparture(jointDepartureOrganizer, Id.create("p2", Person.class)));
		assertEquals(true, jointDepartureOrganizer.scheduledDepartures.size() == 0);
	}

	/**
	 * @return true if the agent has still joint departures left, otherwise false.
	 */
	private boolean peekJointDeparture(JointDepartureOrganizer jointDepartureOrganizer, Id<Person> agentId) {
		Map<Leg, JointDeparture> jointDepartures = jointDepartureOrganizer.scheduledDeparturesMap.get(agentId);
		if (jointDepartures == null || jointDepartures.size() == 0) return false;
		else return true;
	}
	
	private static void assignJointDeparture(QSim qsim, Person person, int legIndex, JointDeparture jointDeparture,
			JointDepartureOrganizer jointDepartureOrganizer) {
		Leg leg = getAgentsLeg(qsim, person, legIndex);
		jointDepartureOrganizer.assignAgentToJointDeparture(person.getId(), leg, jointDeparture);
	}
	
	// The agent gets an unmodifiable copy of the person's plan. Therefore, we have to use the agent's leg for the lookup map.
	private static Leg getAgentsLeg(QSim qsim, Person person, int legIndex) {
		for (MobsimAgent agent : qsim.getAgents()) {
			if (agent.getId().equals(person.getId())) {
				return (Leg) ((PlanAgent) agent).getCurrentPlan().getPlanElements().get(legIndex);
			}
		}
		return null;
	}
	
	private static Scenario makeScenario() {
		Config config = ConfigUtils.createConfig();
		QSimConfigGroup qSimConfigGroup = config.qsim();
		qSimConfigGroup.setEndTime(7200.0);	// stop simulation after 2 hours to prevent dead locks
		Scenario scenario = ScenarioUtils.createScenario(config);
		((NetworkImpl) scenario.getNetwork()).setCapacityPeriod(1.0);
		return scenario;
	}
	
	private static Tuple<QSim, JointDepartureOrganizer> makeQSim(Scenario scenario, EventsManager eventsManager) {
		QSim qSim = new QSim(scenario, eventsManager);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		JointDepartureOrganizer jointDepartureOrganizer = new JointDepartureOrganizer();
		PassengerQNetsimEngine netsimEngine = new PassengerQNetsimEngine(qSim,
				MatsimRandom.getLocalInstance(), jointDepartureOrganizer); 
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		qSim.addDepartureHandler(netsimEngine.getVehicularDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);
		
		return new Tuple<QSim, JointDepartureOrganizer>(qSim, jointDepartureOrganizer);
	}
	
	private static void makeNetwork(Network network) {
		Node node0 = network.getFactory().createNode(Id.create("node0", Node.class), new CoordImpl(0.0, 0.0));
		network.addNode(node0);
		Node node1 = network.getFactory().createNode(Id.create("node1", Node.class), new CoordImpl(100.0,0.0));
		network.addNode(node1);
		Node node2 = network.getFactory().createNode(Id.create("node2", Node.class), new CoordImpl(200.0,0.0));
		network.addNode(node2);
		Node node3 = network.getFactory().createNode(Id.create("node3", Node.class), new CoordImpl(300.0,0.0));
		network.addNode(node3);
		
		makeLink(network, Id.create("0to1", Link.class), node0, node1);
		makeLink(network, Id.create("1to2", Link.class), node1, node2);
		makeLink(network, Id.create("2to3", Link.class), node2, node3);
	}
	
	private static void makeLink(Network network, Id<Link> id, Node from, Node to) {
		Link link = network.getFactory().createLink(id, from, to);
		link.setFreespeed(100);
		link.setNumberOfLanes(1);
		link.setCapacity(10);
		link.setLength(200.0);
		network.addLink(link);
	}

	private static Person makeNonStopDriver(Id<Person> id, Id<Vehicle> vehicleId, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("0to1", Link.class));
		route1.add(Id.create("1to2", Link.class));
		route1.add(Id.create("2to3", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(vehicleId);
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("2to3", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makeDriverA(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("0to1", Link.class));
		route1.add(Id.create("1to2", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(Id.create("v1", Vehicle.class));
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("1to2", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makeDriverB(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("1to2", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("1to2", Link.class));
		route1.add(Id.create("2to3", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(Id.create("v1", Vehicle.class));
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("2to3", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makeDriverThenPassenger(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("0to1", Link.class));
		route1.add(Id.create("1to2", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(Id.create("v0", Vehicle.class));
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("1to2", Link.class));
		a2.setEndTime(activityEndTime);
		plan.addActivity(a2);
		Leg l2 = population.getFactory().createLeg(PassengerDepartureHandler.passengerMode);
		List<Id<Link>> route = new ArrayList<Id<Link>>();
		route.add(Id.create("1to2", Link.class));
		route.add(Id.create("2to3", Link.class));
		NetworkRoute r2 = RouteUtils.createNetworkRoute(route, network);
		r2.setVehicleId(Id.create("v1", Vehicle.class));
		l2.setRoute(r2);
		plan.addLeg(l2);
		Activity a3 = population.getFactory().createActivityFromLinkId("a3", Id.create("2to3", Link.class));
		plan.addActivity(a3);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makeDropOffPickupDriver(Id<Person> id, Population population, Network network, double activityEndTime, double stopEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("0to1", Link.class));
		route1.add(Id.create("1to2", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(Id.create("v1", Vehicle.class));
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("pickup/dropoff", Id.create("1to2", Link.class));
		a2.setEndTime(stopEndTime);
		plan.addActivity(a2);
		Leg l2 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route2 = new ArrayList<Id<Link>>();
		route2.add(Id.create("1to2", Link.class));
		route2.add(Id.create("2to3", Link.class));
		NetworkRoute r2 = RouteUtils.createNetworkRoute(route2, network);
		r2.setVehicleId(Id.create("v1", Vehicle.class));
		l2.setRoute(r2);
		plan.addLeg(l2);
		Activity a3 = population.getFactory().createActivityFromLinkId("a3", Id.create("2to3", Link.class));
		plan.addActivity(a3);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makePickupDropOffDriver(Id<Person> id, Population population, Network network, double activityEndTime, double stopEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l1 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route1 = new ArrayList<Id<Link>>();
		route1.add(Id.create("0to1", Link.class));
		route1.add(Id.create("1to2", Link.class));
		NetworkRoute r1 = RouteUtils.createNetworkRoute(route1, network);
		r1.setVehicleId(Id.create("v1", Vehicle.class));
		l1.setRoute(r1);
		plan.addLeg(l1);		
		Activity a2 = population.getFactory().createActivityFromLinkId("pickup/dropoff", Id.create("1to2", Link.class));
		a2.setEndTime(stopEndTime);
		plan.addActivity(a2);
		Leg l2 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route2 = new ArrayList<Id<Link>>();
		route2.add(Id.create("1to2", Link.class));
		NetworkRoute r2 = RouteUtils.createNetworkRoute(route2, network);
		r2.setVehicleId(Id.create("v1", Vehicle.class));
		l2.setRoute(r2);
		plan.addLeg(l2);
		Activity a3 = population.getFactory().createActivityFromLinkId("pickup/dropoff", Id.create("1to2", Link.class));
		a3.setEndTime(stopEndTime + 1.0);
		plan.addActivity(a3);		
		Leg l3 = population.getFactory().createLeg(PassengerDepartureHandler.driverMode);
		List<Id<Link>> route3 = new ArrayList<Id<Link>>();
		route3.add(Id.create("1to2", Link.class));
		route3.add(Id.create("2to3", Link.class));
		NetworkRoute r3 = RouteUtils.createNetworkRoute(route3, network);
		r3.setVehicleId(Id.create("v1", Vehicle.class));
		l3.setRoute(r3);
		plan.addLeg(l3);
		Activity a4 = population.getFactory().createActivityFromLinkId("a4", Id.create("2to3", Link.class));
		plan.addActivity(a4);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makeNonStopPassenger(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l = population.getFactory().createLeg(PassengerDepartureHandler.passengerMode);
		List<Id<Link>> route = new ArrayList<Id<Link>>();
		route.add(Id.create("0to1", Link.class));
		route.add(Id.create("1to2", Link.class));
		route.add(Id.create("2to3", Link.class));
		NetworkRoute r = RouteUtils.createNetworkRoute(route, network);
		r.setVehicleId(Id.create("v1", Vehicle.class));
		r.setTravelTime(100.0);
		l.setRoute(r);
		plan.addLeg(l);
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("2to3", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}

	private static Person makeDropOffPassenger(Id<Person> id, Population population, Network network, double departureTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("0to1", Link.class));
		a1.setEndTime(departureTime);
		plan.addActivity(a1);
		Leg l = population.getFactory().createLeg(PassengerDepartureHandler.passengerMode);
		List<Id<Link>> route = new ArrayList<Id<Link>>();
		route.add(Id.create("0to1", Link.class));
		route.add(Id.create("1to2", Link.class));
		NetworkRoute r = RouteUtils.createNetworkRoute(route, network);
		r.setVehicleId(Id.create("v1", Vehicle.class));
		l.setRoute(r);
		plan.addLeg(l);
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("1to2", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makePickUpDropOffPassenger(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("1to2", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l = population.getFactory().createLeg(PassengerDepartureHandler.passengerMode);
		List<Id<Link>> route = new ArrayList<Id<Link>>();
		route.add(Id.create("1to2", Link.class));
		NetworkRoute r = RouteUtils.createNetworkRoute(route, network);
		r.setVehicleId(Id.create("v1", Vehicle.class));
		l.setRoute(r);
		plan.addLeg(l);
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("1to2", Link.class));
		a2.setEndTime(activityEndTime + 600.0);
		plan.addActivity(a2);
		Leg l2 = population.getFactory().createLeg(TransportMode.walk);
//		List<Id<Link>> route2 = new ArrayList<Id<Link>>();
//		route2.add(Id.create("1to2", Link.class));
//		route2.add(Id.create("2to3", Link.class));
//		NetworkRoute r2 = RouteUtils.createNetworkRoute(route2, network);
//		r2.setVehicleId(Id.create("v1", Vehicle.class));
		Route r2 = new GenericRouteFactory().createRoute(Id.create("1to2", Link.class), Id.create("2to3", Link.class));
		r2.setTravelTime(100.0);
		l2.setRoute(r2);
		plan.addLeg(l2);
		Activity a3 = population.getFactory().createActivityFromLinkId("a3", Id.create("2to3", Link.class));
		plan.addActivity(a3);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static Person makePickUpPassenger(Id<Person> id, Population population, Network network, double activityEndTime) {
		Person person = population.getFactory().createPerson(id);
		
		Plan plan = population.getFactory().createPlan();
		Activity a1 = population.getFactory().createActivityFromLinkId("a1", Id.create("1to2", Link.class));
		a1.setEndTime(activityEndTime);
		plan.addActivity(a1);
		Leg l = population.getFactory().createLeg(PassengerDepartureHandler.passengerMode);
		List<Id<Link>> route = new ArrayList<Id<Link>>();
		route.add(Id.create("1to2", Link.class));
		route.add(Id.create("2to3", Link.class));
		NetworkRoute r = RouteUtils.createNetworkRoute(route, network);
		r.setVehicleId(Id.create("v1", Vehicle.class));
		l.setRoute(r);
		plan.addLeg(l);
		Activity a2 = population.getFactory().createActivityFromLinkId("a2", Id.create("2to3", Link.class));
		plan.addActivity(a2);
		person.addPlan(plan);
		population.addPerson(person);
		
		return person;
	}
	
	private static class EventsCounter implements PersonEntersVehicleEventHandler, 
		PersonLeavesVehicleEventHandler {

		private int enterCount = 0;
		private int leaveCount = 0;
		
		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent event) {
			enterCount++;
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			leaveCount++;
		}
		
		public int getEnterCount() {
			return this.enterCount;
		}
		
		public int getLeaveCount() {
			return this.leaveCount;
		}
		
	}
	
	private static class EventsPrinter implements BasicEventHandler {

		@Override
		public void reset(final int iter) {
		}

		@Override
		public void handleEvent(final Event event) {
			StringBuilder eventXML = new StringBuilder(180);
			eventXML.append("\t<event ");
			Map<String, String> attr = event.getAttributes();
			for (Map.Entry<String, String> entry : attr.entrySet()) {
				eventXML.append(entry.getKey());
				eventXML.append("=\"");
				eventXML.append(entry.getValue());
				eventXML.append("\" ");
			}
			eventXML.append("/>");
			
//			log.info(eventXML.toString());
			System.out.println(eventXML.toString());
		}
	}
}
