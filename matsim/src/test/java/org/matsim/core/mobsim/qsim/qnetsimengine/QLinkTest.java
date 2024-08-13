/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNetwork;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;


/**
 * NOTE for writing tests - in order to keep compliance with QSim the right sequence must be
 * followed: 1. increase time, 2. move nodes, 3. links sequence.
 * Since QLinkI.getOfferingQLanes().get(0).popFirstVehicle() is a shortcut for moving nodes, it must
 * be executed !!BEFORE!! (not after) QLinkI.doSimStep() (within a sim step)
 * Otherwise QueueWithBuffer for fastCapacityUpdate=true may give wrong results.
 *
 *
 * @author dgrether
 * @author mrieser
 */

public final class QLinkTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private static final Logger logger = LogManager.getLogger( QLinkTest.class );

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testInit(boolean isUsingFastCapacityUpdate) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		assertNotNull(f.qlink1);
		assertEquals(1.0, f.qlink1.getSimulatedFlowCapacityPerTimeStep(), MatsimTestUtils.EPSILON);
		assertEquals(1.0, f.qlink1.getSpaceCap(), MatsimTestUtils.EPSILON);
		// TODO dg[april2008] this assertions are not covering everything in
		// QueueLink's constructor.
		// Extend the tests by checking the methods initFlowCapacity and
		// recalcCapacity
		assertEquals(f.link1, f.qlink1.getLink());
		assertEquals(f.queueNetwork.getNetsimNode(Id.create("2", Node.class)), f.qlink1.getToNode());
	}


	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testAdd(boolean isUsingFastCapacityUpdate) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		QVehicle v = new QVehicleImpl(f.basicVehicle);

		Person p = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		Plan plan = PopulationUtils.createPlan();
		plan.addActivity(PopulationUtils.createActivityFromLinkId("type", Id.createLinkId("0")));
		p.addPlan(plan);
		v.setDriver(createAndInsertPersonDriverAgentImpl(p, f.sim));

		f.qlink1.getAcceptingQLane().addFromUpstream(v);
		assertEquals(1, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertFalse(f.qlink1.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(f.qlink1.isNotOfferingVehicle());
	}


	private static PersonDriverAgentImpl createAndInsertPersonDriverAgentImpl(Person p, QSim simulation) {
		PersonDriverAgentImpl agent = new PersonDriverAgentImpl(p.getSelectedPlan(), simulation, simulation.getChildInjector().getInstance(TimeInterpretation.class));
		simulation.insertAgentIntoMobsim(agent);
		return agent;
	}

	/**
	 * Tests that vehicles driving on a link are found with {@link NetsimLink#getAllVehicles()}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testGetVehicle_Driving(boolean isUsingFastCapacityUpdate) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);

		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		Person p = PopulationUtils.getFactory().createPerson(Id.create(23, Person.class));
		Plan plan = PopulationUtils.createPlan();
		p.addPlan(plan);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("home", f.link1.getId()));
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), f.link2.getId()));
		plan.addLeg(leg);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", f.link2.getId()));
		PersonDriverAgentImpl driver = createAndInsertPersonDriverAgentImpl(p, f.sim);
		veh.setDriver(driver);

		double now = 0. ;

		driver.endActivityAndComputeNextState( now );

		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertNull(f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());

		// add a vehicle, it should be now in the vehicle queue
		f.qlink1.getAcceptingQLane().addFromUpstream(veh);
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(1, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertEquals(veh, f.qlink1.getVehicle(id1), "vehicle not found on link.");
		assertEquals(1, f.qlink1.getAllVehicles().size());

		now = 1. ;
		f.sim.getSimTimer().setTime(now);

		// time step 1, vehicle should be now in the buffer
		f.qlink1.doSimStep();
		assertFalse(f.qlink1.isNotOfferingVehicle());
//		assertEquals(0, ((QueueWithBuffer) f.qlink1.qlane).vehInQueueCount());
//		assertEquals("vehicle not found in buffer.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		now = 2. ;
		f.sim.getSimTimer().setTime(now);
		f.qlink1.doSimStep();

        // time step 3, vehicle leaves link
        now = 3. ;
        f.sim.getSimTimer().setTime(now);
		assertEquals(veh, f.qlink1.getOfferingQLanes().get(0).popFirstVehicle());
		assertTrue(f.qlink1.isNotOfferingVehicle());
//		assertEquals(0, ((QueueWithBuffer) f.qlink1.qlane).vehInQueueCount());
		assertNull(f.qlink1.getVehicle(id1), "vehicle should not be on link anymore.");
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles parked on a link are found with {@link NetsimLink#getAllVehicles()}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testGetVehicle_Parking(boolean isUsingFastCapacityUpdate) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);

		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		Person p = PopulationUtils.getFactory().createPerson(Id.create(42, Person.class));
		Plan plan = PopulationUtils.createPlan();
		plan.addActivity(PopulationUtils.createActivityFromLinkId("type", Id.createLinkId("0")));
		p.addPlan(plan);
		veh.setDriver(createAndInsertPersonDriverAgentImpl(p, f.sim));

		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertEquals(0, f.qlink1.getAllVehicles().size());

		f.qlink1.addParkedVehicle(veh);
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size()); // vehicle not on _lane_
		assertEquals(veh, f.qlink1.getVehicle(id1), "vehicle not found in parking list.");
		assertEquals(1, f.qlink1.getAllVehicles().size()); // vehicle indeed on _link_
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		assertEquals(veh, f.qlink1.removeParkedVehicle(veh.getId()), "removed wrong vehicle.");
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertNull(f.qlink1.getVehicle(id1), "vehicle not found in parking list.");
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles departing on a link are found with {@link NetsimLink#getAllVehicles()}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testGetVehicle_Departing(boolean isUsingFastCapacityUpdate) {
		Fixture f = new Fixture(isUsingFastCapacityUpdate);
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);


		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		Person pers = PopulationUtils.getFactory().createPerson(Id.create(80, Person.class));
		Plan plan = PopulationUtils.createPlan();
		pers.addPlan(plan);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("home", f.link1.getId()));
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), f.link2.getId());
		route.setVehicleId(f.basicVehicle.getId());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", f.link2.getId()));
		PersonDriverAgentImpl driver = createAndInsertPersonDriverAgentImpl(pers, f.sim);
		veh.setDriver(driver);
		driver.setVehicle(veh);
		f.qlink1.addParkedVehicle(veh); // vehicle parked

		double now = 0. ;
		f.sim.getSimTimer().setTime(now);

		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertEquals(1, f.qlink1.getAllVehicles().size());

		driver.endActivityAndComputeNextState(0);
		f.queueNetwork.simEngine.getNetsimInternalInterface().arrangeNextAgentState(driver) ; // i.e. driver departs, should now be in wait queue
		assertTrue(f.qlink1.isNotOfferingVehicle()); // veh not in buffer
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size()); // veh not on lane
		assertEquals(veh, f.qlink1.getVehicle(id1), "vehicle not found in waiting list."); // veh _should_ be on link (in waiting list)
		assertEquals(1, f.qlink1.getAllVehicles().size()); // dto
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next()); // dto

		now = 1. ;
		f.sim.getSimTimer().setTime(now);

		// time step 1, vehicle should be now in the buffer
		f.qlink1.doSimStep();
		assertFalse(f.qlink1.isNotOfferingVehicle()); // i.e. is offering the vehicle
		assertEquals(1, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size()); // somewhere on lane
		assertEquals(veh, f.qlink1.getVehicle(id1), "vehicle not found in buffer."); // somewhere on link
		assertEquals(1, f.qlink1.getAllVehicles().size()); // somewhere on link

        now = 2. ;
        f.sim.getSimTimer().setTime(now);

        // vehicle leaves link
		assertEquals(veh, f.qlink1.getOfferingQLanes().get(0).popFirstVehicle());
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.getAcceptingQLane()).getAllVehicles().size());
		assertNull(f.qlink1.getVehicle(id1), "vehicle should not be on link anymore.");
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests the behavior of the buffer (e.g. that it does not accept too many vehicles).
	 *
	 * @author mrieser
	 */

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testBuffer(boolean isUsingFastCapacityUpdate) {
		Config conf = utils.loadConfig((String)null);
		conf.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		conf.qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(conf);

		Network network = (Network) scenario.getNetwork();
		network.setCapacityPeriod(1.0);
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(2, 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1.0, 1.0, 1.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 1.0, 1.0, 1.0, 1.0 );

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim qsim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		NetsimNetwork queueNetwork = qsim.getNetsimNetwork();
		dummify((QNetwork) queueNetwork);
		QLinkImpl qlink = (QLinkImpl) queueNetwork.getNetsimLink(Id.create("1", Link.class));

		QVehicle v1 = new QVehicleImpl(
			  VehicleUtils.createVehicle(Id.create("1", Vehicle.class ),
				    VehicleUtils.createVehicleType(Id.create("defaultVehicleType", VehicleType.class ) ) ) );
		Person p = createPerson(Id.createPersonId(1), scenario, link1, link2);
		PersonDriverAgentImpl pa1 = createAndInsertPersonDriverAgentImpl(p, qsim);
		v1.setDriver(pa1);
		pa1.setVehicle(v1);
		pa1.endActivityAndComputeNextState(0);

		QVehicle v2 = new QVehicleImpl(
			  VehicleUtils.createVehicle(Id.create("2", Vehicle.class ),
				    VehicleUtils.createVehicleType(Id.create("defaultVehicleType", VehicleType.class ) ) ) );
		Person p2 = createPerson( Id.createPersonId(2),scenario,link1, link2) ;
		PersonDriverAgentImpl pa2 = createAndInsertPersonDriverAgentImpl(p2, qsim);
		v2.setDriver(pa2);
		pa2.setVehicle(v2);

		double now = 0. ;

		pa2.endActivityAndComputeNextState( now );


		// start test
		assertTrue(qlink.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		// add v1
		qlink.getAcceptingQLane().addFromUpstream(v1);
		assertEquals(1, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertTrue(qlink.isNotOfferingVehicle());

		now = 1. ;
		qsim.getSimTimer().setTime(1.);

		// time step 1, v1 is moved to buffer
		qlink.doSimStep();
		assertEquals(1, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertFalse(qlink.isNotOfferingVehicle());
		// add v2, still time step 1
		qlink.getAcceptingQLane().addFromUpstream(v2);
		assertEquals(2, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertFalse(qlink.isNotOfferingVehicle());

		now = 2. ;
		qsim.getSimTimer().setTime(2.);

		// time step 2, v1 still in buffer, v2 cannot enter buffer, so still on link
		qlink.doSimStep( );
		assertEquals(2, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertFalse(qlink.isNotOfferingVehicle());

		//time step 3
        now = 3. ;
        qsim.getSimTimer().setTime(3.);

		// v1 leaves buffer
		assertEquals(v1, qlink.getOfferingQLanes().get(0).popFirstVehicle());
		assertEquals(1, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertTrue(qlink.isNotOfferingVehicle());

		//v2 moves to buffer
		qlink.doSimStep();
		assertEquals(1, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertFalse(qlink.isNotOfferingVehicle());

        // time step 4
        now = 4. ;
        qsim.getSimTimer().setTime(4.);

        // v2 leaves buffer
		assertEquals(v2, qlink.getOfferingQLanes().get(0).popFirstVehicle());
		assertEquals(0, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertTrue(qlink.isNotOfferingVehicle());

		//empty link
		qlink.doSimStep();
		assertEquals(0, ((QueueWithBuffer) qlink.getAcceptingQLane()).getAllVehicles().size());
		assertTrue(qlink.isNotOfferingVehicle());
	}


	private static Person createPerson(Id<Person> personId, MutableScenario scenario, Link link1, Link link2) {
		final Id<Person> id = personId;
		Person p = PopulationUtils.getFactory().createPerson(id);
		Plan plan = PersonUtils.createAndAddPlan(p, true);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		NetworkRoute route = ((PopulationFactory) scenario.getPopulation().getFactory()).getRouteFactories().createRoute(NetworkRoute.class, link1.getId(), link2.getId());
		leg.setRoute(route);
		route.setLinkIds(link1.getId(), null, link2.getId());
		leg.setRoute(route);
		PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link2.getId());
		return p;
	}


	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testStorageSpaceDifferentVehicleSizes(boolean isUsingFastCapacityUpdate) {

		Fixture f = new Fixture(isUsingFastCapacityUpdate);

		VehicleType defaultVehType = VehicleUtils.createVehicleType(Id.create("defaultVehicleType", VehicleType.class ) );
		VehicleType mediumVehType = VehicleUtils.createVehicleType(Id.create("mediumVehicleType", VehicleType.class ) );
		mediumVehType.setPcuEquivalents(2.5);
		VehicleType largeVehType = VehicleUtils.createVehicleType(Id.create("largeVehicleType", VehicleType.class ) );
		largeVehType.setPcuEquivalents(5);

		double now = 0.0 ;
		f.sim.getSimTimer().setTime(now);

		QVehicle veh1 = new QVehicleImpl( VehicleUtils.createVehicle(Id.create(1, Vehicle.class ), defaultVehType ) );
		Person p1 = createPerson2(Id.createPersonId(5), f);
		PersonDriverAgentImpl driver1 = createAndInsertPersonDriverAgentImpl(p1, f.sim);
		veh1.setDriver(driver1);
		driver1.setVehicle(veh1);
		driver1.endActivityAndComputeNextState( now );
		QVehicle veh25 = new QVehicleImpl( VehicleUtils.createVehicle(Id.create(2, Vehicle.class ), mediumVehType ) );
		Person p2 = createPerson2(Id.createPersonId(6), f);
		PersonDriverAgentImpl driver25 = createAndInsertPersonDriverAgentImpl(p2, f.sim);
		veh25.setDriver(driver25);
		driver25.setVehicle(veh25);
		driver25.endActivityAndComputeNextState( now );
		QVehicle veh5 = new QVehicleImpl( VehicleUtils.createVehicle(Id.create(3, Vehicle.class ), largeVehType ) );
		Person p3 = createPerson2(Id.createPersonId(7), f);
		PersonDriverAgentImpl driver5 = createAndInsertPersonDriverAgentImpl(p3, f.sim);
		veh5.setDriver(driver5);
		driver5.setVehicle(veh5);
		driver5.endActivityAndComputeNextState( now );

		assertEquals(10.0, f.qlink2.getSpaceCap(), MatsimTestUtils.EPSILON, "wrong initial storage capacity.");
		f.qlink2.getAcceptingQLane().addFromUpstream(veh5);  // used vehicle equivalents: 5
		assertTrue(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		f.qlink2.getAcceptingQLane().addFromUpstream(veh5);  // used vehicle equivalents: 10
		assertFalse(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(f.qlink2.isNotOfferingVehicle());

		for (int t = 0; t < 5; t++) {
	      now = t;
	      f.sim.getSimTimer().setTime(now);
          f.qlink2.doSimStep();
		}

		now = 5.0 ;
		f.sim.getSimTimer().setTime(now);
		f.qlink2.doSimStep(); // first veh moves to buffer, used vehicle equivalents: 5
		assertTrue(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		assertFalse(f.qlink2.isNotOfferingVehicle());

        now = 6.0 ;
        f.sim.getSimTimer().setTime(now);
        // popFirstVehicle() is a shortcut for moveNodes(), and therefore must be executed
        //!!BEFORE!! qlink.doSimStep() (within a sim step)
        f.qlink2.getOfferingQLanes().get(0).popFirstVehicle();  // first veh leaves buffer
        f.qlink2.doSimStep(); // used vehicle equivalents: 5.0

        assertTrue(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		f.qlink2.getAcceptingQLane().addFromUpstream(veh25); // used vehicle equivalents: 7.5
		f.qlink2.getAcceptingQLane().addFromUpstream(veh1);  // used vehicle equivalents: 8.5
		f.qlink2.getAcceptingQLane().addFromUpstream(veh1);  // used vehicle equivalents: 9.5
		assertTrue(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		f.qlink2.getAcceptingQLane().addFromUpstream(veh1);  // used vehicle equivalents: 10.5
		assertFalse(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());

        for (int i = 7; i < 10 ; i++) {
            now = i;
            f.sim.getSimTimer().setTime(now);
            f.qlink2.doSimStep();
            assertFalse(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
          }

        now = 10.0 ;
        f.sim.getSimTimer().setTime(now);
        f.qlink2.doSimStep(); // "2nd first" veh moves to buffer, used vehicle equivalents: 5.5

        assertTrue(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
		f.qlink2.getAcceptingQLane().addFromUpstream(veh1);  // used vehicle equivalents: 6.5
		f.qlink2.getAcceptingQLane().addFromUpstream(veh25); // used vehicle equivalents: 9.0
		f.qlink2.getAcceptingQLane().addFromUpstream(veh1);  // used vehicle equivalents: 10.0
		assertFalse(f.qlink2.getAcceptingQLane().isAcceptingFromUpstream());
	}


	private Person createPerson2(Id<Person> personId, Fixture f) {
		final Id<Person> id = personId;
		Person p = PopulationUtils.getFactory().createPerson(id);
		Plan plan = PopulationUtils.createPlan();
		p.addPlan(plan);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("home", f.link1.getId()));
		Leg leg = PopulationUtils.createLeg(TransportMode.car);
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(f.link1.getId(), f.link2.getId());
		route.setVehicleId(f.basicVehicle.getId());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", f.link2.getId()));
		return p;
	}


	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testStuckEvents(boolean isUsingFastCapacityUpdate) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().qsim().setStuckTime(100);
		scenario.getConfig().qsim().setRemoveStuckVehicles(true);
		scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		MatsimRandom.reset(4711); // yyyyyy !!!!!!
		Network network = (Network) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(1001, 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1.0, 1.0, 3600.0, 1.0 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		Link link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 10 * 7.5, 7.5, 3600.0, 1.0 );
		final Node fromNode2 = node2;
		final Node toNode2 = node2;
		Link link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, 2 * 7.5, 7.5, 3600.0, 1.0 );

		for (int i = 0; i < 5; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PopulationUtils.createPlan();
			Activity act = PopulationUtils.createActivityFromLinkId("h", link1.getId());
			act.setEndTime(7*3600);
			plan.addActivity(act);
			Leg leg = PopulationUtils.createLeg("car");
			NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link2.getId());
			List<Id<Link>> links = new ArrayList<Id<Link>>();
			links.add(link3.getId()); // let the person(s) drive around in circles to generate a traffic jam
			links.add(link3.getId());
			links.add(link3.getId());
			links.add(link3.getId());
			route.setLinkIds(link1.getId(), links, link2.getId());
			leg.setRoute(route);
			plan.addLeg(leg);
			plan.addActivity(PopulationUtils.createActivityFromLinkId("w", link2.getId()));
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim sim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);

		EventsCollector collector = new EventsCollector();
		sim.getEventsManager().addHandler(collector);

		sim.run();

		int stuckCnt = 0;
		for (Event e : collector.getEvents()) {
			logger.warn( e ) ;
			if (e instanceof PersonStuckEvent) {
				stuckCnt++;
			}
		}

		assertEquals(3, stuckCnt);
	}

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	private static final class Fixture {
		/*package*/ final MutableScenario scenario;
		/*package*/ final Link link1;
		/*package*/ final Link link2;
		/*package*/ final QNetwork queueNetwork;
		/*package*/ final QLinkImpl qlink1;
		/*package*/ final QLinkImpl qlink2;
		/*package*/ final Vehicle basicVehicle;
		/*package*/ final QSim sim;

		/*package*/ Fixture(boolean usingFastCapacityUpdate) {
			this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().qsim().setStuckTime(100);
			this.scenario.getConfig().qsim().setRemoveStuckVehicles(true);
			this.scenario.getConfig().qsim().setUsingFastCapacityUpdate(usingFastCapacityUpdate);
			this.scenario.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

			Network network = (Network) this.scenario.getNetwork();
			network.setCapacityPeriod(3600.0);
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(1, 0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(1001, 0));
			final Node fromNode = node1;
			final Node toNode = node2;
			this.link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1.0, 1.0, 3600.0, 1.0 );
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			this.link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 10 * 7.5, 2.0 * 7.5, 3600.0, 1.0 );
			EventsManager eventsManager = EventsUtils.createEventsManager();
			PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
			sim = new QSimBuilder(scenario.getConfig()) //
					.useDefaults() //
					.build(scenario, eventsManager);
			this.queueNetwork = (QNetwork) sim.getNetsimNetwork();

            this.qlink1 = (QLinkImpl) this.queueNetwork.getNetsimLink(Id.create("1", Link.class));
            this.qlink2 = (QLinkImpl) this.queueNetwork.getNetsimLink(Id.create("2", Link.class));
            dummify(this.queueNetwork);
			this.basicVehicle = VehicleUtils.createVehicle(Id.create("1", Vehicle.class ),
				  VehicleUtils.createVehicleType(Id.create("defaultVehicleType", VehicleType.class ) ) );
		}

	}

    private static void dummify(QNetwork network) {
        NetElementActivationRegistry netElementActivator = new NetElementActivationRegistry() {
            @Override
            protected void registerNodeAsActive(QNodeI node) {

            }

            @Override
            int getNumberOfSimulatedNodes() {
                return 0;
            }

            @Override
            protected void registerLinkAsActive(QLinkI link) {

            }

            @Override
            int getNumberOfSimulatedLinks() {
                return 0;
            }
        };
        for (QNodeI node : network.getNetsimNodes().values()) {
        	if ( node instanceof QNodeImpl ) {
        		((QNodeImpl) node).setNetElementActivationRegistry(netElementActivator);
        	}
        }
        for (QLinkI link : network.getNetsimLinks().values()) {
        	if ( link instanceof QLinkImpl ) {
        		((QLinkImpl) link).setNetElementActivationRegistry(netElementActivator);
        	}
        }
    }

}
