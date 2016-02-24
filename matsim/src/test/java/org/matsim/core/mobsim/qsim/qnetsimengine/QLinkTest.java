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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * @author dgrether
 * @author mrieser
 */
public final class QLinkTest extends MatsimTestCase {

	public void testInit() {
		Fixture f = new Fixture();
		assertNotNull(f.qlink1);
		assertEquals(1.0, f.qlink1.getSimulatedFlowCapacity(), EPSILON);
		assertEquals(1.0, f.qlink1.getSpaceCap(), EPSILON);
		// TODO dg[april2008] this assertions are not covering everything in
		// QueueLink's constructor.
		// Extend the tests by checking the methods initFlowCapacity and
		// recalcCapacity
		assertEquals(f.link1, f.qlink1.getLink());
		assertEquals(f.queueNetwork.getNetsimNode(Id.create("2", Node.class)), f.qlink1.getToNode());
	}


	public void testAdd() {
		Fixture f = new Fixture();
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		QVehicle v = new QVehicle(f.basicVehicle);

		Person p = PopulationUtils.createPerson(Id.create("1", Person.class));
		p.addPlan(new PlanImpl());
		v.setDriver(createAndInsertPersonDriverAgentImpl(p, f.sim));

		f.qlink1.addFromUpstream(v);
		assertEquals(1, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertFalse(f.qlink1.isAcceptingFromUpstream());
		assertTrue(f.qlink1.isNotOfferingVehicle());
	}


	private static PersonDriverAgentImpl createAndInsertPersonDriverAgentImpl(Person p, QSim simulation) {
		PersonDriverAgentImpl agent = new PersonDriverAgentImpl(p.getSelectedPlan(), simulation);
		simulation.insertAgentIntoMobsim(agent); 
		return agent;
	}

	/**
	 * Tests that vehicles driving on a link are found with {@link NetsimLink#getVehicle(Id)}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */
	public void testGetVehicle_Driving() {
		Fixture f = new Fixture();
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);

		QVehicle veh = new QVehicle(f.basicVehicle);
		Person p = PopulationUtils.createPerson(Id.create(23, Person.class));
		PlanImpl plan = new PlanImpl();
		p.addPlan(plan);
		plan.addActivity(new ActivityImpl("home", f.link1.getId()));
		Leg leg = new LegImpl(TransportMode.car);
		leg.setRoute(new LinkNetworkRouteImpl(f.link1.getId(), f.link2.getId()));
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", f.link2.getId()));
		PersonDriverAgentImpl driver = createAndInsertPersonDriverAgentImpl(p, f.sim);
		veh.setDriver(driver);
		driver.endActivityAndComputeNextState(0);
		
		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertNull(f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());

		// add a vehicle, it should be now in the vehicle queue
		f.qlink1.addFromUpstream(veh);
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(1, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals("vehicle not found on link.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());

		// time step 1, vehicle should be now in the buffer
		f.qlink1.doSimStep(1.0);
		assertFalse(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		// time step 2, vehicle leaves link
		f.qlink1.doSimStep(2.0);
		assertEquals(veh, f.qlink1.popFirstVehicle());
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertNull("vehicle should not be on link anymore.", f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles parked on a link are found with {@link NetsimLink#getVehicle(Id)}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */
	public void testGetVehicle_Parking() {
		Fixture f = new Fixture();
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);

		QVehicle veh = new QVehicle(f.basicVehicle);
		Person p = PopulationUtils.createPerson(Id.create(42, Person.class));
		p.addPlan(new PlanImpl());
		veh.setDriver(createAndInsertPersonDriverAgentImpl(p, f.sim));

		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals(0, f.qlink1.getAllVehicles().size());

		f.qlink1.addParkedVehicle(veh);
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals("vehicle not found in parking list.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		assertEquals("removed wrong vehicle.", veh, f.qlink1.removeParkedVehicle(veh.getId()));
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertNull("vehicle not found in parking list.", f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests that vehicles departing on a link are found with {@link NetsimLink#getVehicle(Id)}
	 * and {@link NetsimLink#getAllVehicles()}.
	 *
	 * @author mrieser
	 */
	public void testGetVehicle_Departing() {
		Fixture f = new Fixture();
		Id<Vehicle> id1 = Id.create("1", Vehicle.class);


		QVehicle veh = new QVehicle(f.basicVehicle);
		Person pers = PopulationUtils.createPerson(Id.create(80, Person.class));
		Plan plan = new PlanImpl();
		pers.addPlan(plan);
		plan.addActivity(new ActivityImpl("home", f.link1.getId()));
		Leg leg = new LegImpl(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.link1.getId(), f.link2.getId());
		route.setVehicleId(f.basicVehicle.getId());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", f.link2.getId()));
		PersonDriverAgentImpl driver = createAndInsertPersonDriverAgentImpl(pers, f.sim);
		veh.setDriver(driver);
		driver.setVehicle(veh);
		f.qlink1.addParkedVehicle(veh);

		// start test, check initial conditions
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals(1, f.qlink1.getAllVehicles().size());

		driver.endActivityAndComputeNextState(0);
		f.queueNetwork.simEngine.internalInterface.arrangeNextAgentState(driver) ;
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals("vehicle not found in waiting list.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		// time step 1, vehicle should be now in the buffer
		f.qlink1.doSimStep(1.0);
		assertFalse(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());

		// vehicle leaves link
		assertEquals(veh, f.qlink1.popFirstVehicle());
		assertTrue(f.qlink1.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertNull("vehicle should not be on link anymore.", f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());
	}

	/**
	 * Tests the behavior of the buffer (e.g. that it does not accept too many vehicles).
	 *
	 * @author mrieser
	 */
	public void testBuffer() {
		Config conf = super.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(conf);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(1.0);
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(1, 0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(2, 0));
		Link link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1.0, 1.0, 1.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1.0, 1.0, 1.0, 1.0);
		QSim qsim = QSimUtils.createDefaultQSim(scenario, (EventsUtils.createEventsManager()));
		NetsimNetwork queueNetwork = qsim.getNetsimNetwork();
		dummify((QNetwork) queueNetwork);
		QLinkImpl qlink = (QLinkImpl) queueNetwork.getNetsimLink(Id.create("1", Link.class));

		QVehicle v1 = new QVehicle(new VehicleImpl(Id.create("1", Vehicle.class), new VehicleTypeImpl(Id.create("defaultVehicleType", VehicleType.class))));
		Person p = createPerson(Id.createPersonId(1), scenario, link1, link2);
		PersonDriverAgentImpl pa1 = createAndInsertPersonDriverAgentImpl(p, qsim);
		v1.setDriver(pa1);
		pa1.setVehicle(v1);
		pa1.endActivityAndComputeNextState(0);

		QVehicle v2 = new QVehicle(new VehicleImpl(Id.create("2", Vehicle.class), new VehicleTypeImpl(Id.create("defaultVehicleType", VehicleType.class))));
		Person p2 = createPerson( Id.createPersonId(2),scenario,link1, link2) ;
		PersonDriverAgentImpl pa2 = createAndInsertPersonDriverAgentImpl(p2, qsim);
		v2.setDriver(pa2);
		pa2.setVehicle(v2);
		pa2.endActivityAndComputeNextState(0);


		// start test
		assertTrue(qlink.isNotOfferingVehicle());
		assertEquals(0, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		// add v1
		qlink.addFromUpstream(v1);
		assertEquals(1, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertTrue(qlink.isNotOfferingVehicle());
		// time step 1, v1 is moved to buffer
		qlink.doSimStep(1.0);
		assertEquals(0, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertFalse(qlink.isNotOfferingVehicle());
		// add v2, still time step 1
		qlink.addFromUpstream(v2);
		assertEquals(1, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertFalse(qlink.isNotOfferingVehicle());
		// time step 2, v1 still in buffer, v2 cannot enter buffer, so still on link
		qlink.doSimStep(2.0);
		assertEquals(1, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertFalse(qlink.isNotOfferingVehicle());
		// v1 leaves buffer
		assertEquals(v1, qlink.popFirstVehicle());
		assertEquals(1, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertTrue(qlink.isNotOfferingVehicle());
		// time step 3, v2 moves to buffer
		qlink.doSimStep(3.0);
		assertEquals(0, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertFalse(qlink.isNotOfferingVehicle());
		// v2 leaves buffer
		assertEquals(v2, qlink.popFirstVehicle());
		assertEquals(0, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertTrue(qlink.isNotOfferingVehicle());
		// time step 4, empty link
		qlink.doSimStep(4.0);
		assertEquals(0, ((QueueWithBuffer) qlink.road).vehInQueueCount());
		assertTrue(qlink.isNotOfferingVehicle());
	}


	private static Person createPerson(Id<Person> personId, MutableScenario scenario, Link link1, Link link2) {
		Person p = PopulationUtils.createPerson(personId);
		PlanImpl plan = PersonUtils.createAndAddPlan(p, true);
		plan.createAndAddActivity("h", link1.getId());
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).createRoute(NetworkRoute.class, link1.getId(), link2.getId());
		leg.setRoute(route);
		route.setLinkIds(link1.getId(), null, link2.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", link2.getId());
		return p;
	}

	public void testStorageSpaceDifferentVehicleSizes() {
		Fixture f = new Fixture();

		VehicleType defaultVehType = new VehicleTypeImpl(Id.create("defaultVehicleType", VehicleType.class));
		VehicleType mediumVehType = new VehicleTypeImpl(Id.create("mediumVehicleType", VehicleType.class));
		mediumVehType.setPcuEquivalents(2.5);
		VehicleType largeVehType = new VehicleTypeImpl(Id.create("largeVehicleType", VehicleType.class));
		largeVehType.setPcuEquivalents(5);
		QVehicle veh1 = new QVehicle(new VehicleImpl(Id.create(1, Vehicle.class), defaultVehType));
		Person p1 = createPerson2(Id.createPersonId(5), f);
		PersonDriverAgentImpl driver1 = createAndInsertPersonDriverAgentImpl(p1, f.sim);
		veh1.setDriver(driver1);
		driver1.setVehicle(veh1);
		driver1.endActivityAndComputeNextState(0.0);
		QVehicle veh25 = new QVehicle(new VehicleImpl(Id.create(2, Vehicle.class), mediumVehType));
		Person p2 = createPerson2(Id.createPersonId(6), f);
		PersonDriverAgentImpl driver25 = createAndInsertPersonDriverAgentImpl(p2, f.sim);
		veh25.setDriver(driver25);
		driver25.setVehicle(veh25);
		driver25.endActivityAndComputeNextState(0.0);
		QVehicle veh5 = new QVehicle(new VehicleImpl(Id.create(3, Vehicle.class), largeVehType));
		Person p3 = createPerson2(Id.createPersonId(7), f);
		PersonDriverAgentImpl driver5 = createAndInsertPersonDriverAgentImpl(p3, f.sim);
		veh5.setDriver(driver5);
		driver5.setVehicle(veh5);
		driver5.endActivityAndComputeNextState(0.0);

		assertEquals("wrong initial storage capacity.", 10.0, f.qlink2.getSpaceCap(), EPSILON);
		f.qlink2.addFromUpstream(veh5);  // used vehicle equivalents: 5
		assertTrue(f.qlink2.isAcceptingFromUpstream());
		f.qlink2.addFromUpstream(veh5);  // used vehicle equivalents: 10
		assertFalse(f.qlink2.isAcceptingFromUpstream());

		assertTrue(f.qlink2.isNotOfferingVehicle());
		f.qlink2.doSimStep(5.0); // first veh moves to buffer, used vehicle equivalents: 5
		assertTrue(f.qlink2.isAcceptingFromUpstream());
		assertFalse(f.qlink2.isNotOfferingVehicle());
		f.qlink2.popFirstVehicle();  // first veh leaves buffer
		assertTrue(f.qlink2.isAcceptingFromUpstream());

		f.qlink2.addFromUpstream(veh25); // used vehicle equivalents: 7.5
		f.qlink2.addFromUpstream(veh1);  // used vehicle equivalents: 8.5
		f.qlink2.addFromUpstream(veh1);  // used vehicle equivalents: 9.5
		assertTrue(f.qlink2.isAcceptingFromUpstream());
		f.qlink2.addFromUpstream(veh1);  // used vehicle equivalents: 10.5
		assertFalse(f.qlink2.isAcceptingFromUpstream());

		f.qlink2.doSimStep(6.0); // first veh moves to buffer, used vehicle equivalents: 5.5
		assertTrue(f.qlink2.isAcceptingFromUpstream());
		f.qlink2.addFromUpstream(veh1);  // used vehicle equivalents: 6.5
		f.qlink2.addFromUpstream(veh25); // used vehicle equivalents: 9.0
		f.qlink2.addFromUpstream(veh1);  // used vehicle equivalents: 10.0
		assertFalse(f.qlink2.isAcceptingFromUpstream());
	}


	private Person createPerson2(Id<Person> personId, Fixture f) {
		Person p = PopulationUtils.createPerson(personId);
		Plan plan = new PlanImpl();
		p.addPlan(plan);
		plan.addActivity(new ActivityImpl("home", f.link1.getId()));
		Leg leg = new LegImpl(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.link1.getId(), f.link2.getId());
		route.setVehicleId(f.basicVehicle.getId());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", f.link2.getId()));
		return p;
	}

	public void testStuckEvents() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().qsim().setStuckTime(100);
		scenario.getConfig().qsim().setRemoveStuckVehicles(true);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(1, 0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(1001, 0));
		Link link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1.0, 1.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 10 * 7.5, 7.5, 3600.0, 1.0);
		Link link3 = network.createAndAddLink(Id.create("3", Link.class), node2, node2, 2 * 7.5, 7.5, 3600.0, 1.0);

		for (int i = 0; i < 5; i++) {
			Person p = PopulationUtils.createPerson(Id.create(i, Person.class));
			PlanImpl plan = new PlanImpl();
			Activity act = new ActivityImpl("h", link1.getId());
			act.setEndTime(7*3600);
			plan.addActivity(act);
			Leg leg = new LegImpl("car");
			NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
			List<Id<Link>> links = new ArrayList<Id<Link>>();
			links.add(link3.getId()); // let the person(s) drive around in circles to generate a traffic jam
			links.add(link3.getId());
			links.add(link3.getId());
			links.add(link3.getId());
			route.setLinkIds(link1.getId(), links, link2.getId());
			leg.setRoute(route);
			plan.addLeg(leg);
			plan.addActivity(new ActivityImpl("w", link2.getId()));
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
		}

		QSim sim = QSimUtils.createDefaultQSim(scenario, EventsUtils.createEventsManager());

		EventsCollector collector = new EventsCollector();
		sim.getEventsManager().addHandler(collector);

		sim.run();

		int stuckCnt = 0;
		for (Event e : collector.getEvents()) {
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

		/*package*/ Fixture() {
			this.scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().qsim().setStuckTime(100);
			this.scenario.getConfig().qsim().setRemoveStuckVehicles(true);
			NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
			network.setCapacityPeriod(3600.0);
			Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord(0, 0));
			Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord(1, 0));
			Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord(1001, 0));
			this.link1 = network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1.0, 1.0, 3600.0, 1.0);
			this.link2 = network.createAndAddLink(Id.create("2", Link.class), node2, node3, 10 * 7.5, 2.0 * 7.5, 3600.0, 1.0);
			sim = QSimUtils.createDefaultQSim(scenario, (EventsUtils.createEventsManager()));
			this.queueNetwork = (QNetwork) sim.getNetsimNetwork();

            this.qlink1 = (QLinkImpl) this.queueNetwork.getNetsimLink(Id.create("1", Link.class));
            this.qlink2 = (QLinkImpl) this.queueNetwork.getNetsimLink(Id.create("2", Link.class));
            dummify(this.queueNetwork);
			this.basicVehicle = new VehicleImpl(Id.create("1", Vehicle.class), new VehicleTypeImpl(Id.create("defaultVehicleType", VehicleType.class)));
		}

	}

    private static void dummify(QNetwork network) {
        NetElementActivator netElementActivator = new NetElementActivator() {
            @Override
            protected void activateNode(QNode node) {

            }

            @Override
            int getNumberOfSimulatedNodes() {
                return 0;
            }

            @Override
            protected void activateLink(QLinkInternalI link) {

            }

            @Override
            int getNumberOfSimulatedLinks() {
                return 0;
            }
        };
        for (QNode node : network.getNetsimNodes().values()) {
            node.setNetElementActivator(netElementActivator);
        }
        for (QLinkInternalI link : network.getNetsimLinks().values()) {
            ((QLinkImpl) link).setNetElementActivator(netElementActivator);
        }
    }

}
