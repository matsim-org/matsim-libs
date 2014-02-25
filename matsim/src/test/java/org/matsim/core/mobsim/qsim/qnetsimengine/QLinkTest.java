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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
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
public class QLinkTest extends MatsimTestCase {

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
		assertEquals(f.queueNetwork.getNetsimNode(new IdImpl("2")), f.qlink1.getToNode());
	}


	public void testAdd() {
		Fixture f = new Fixture();
		assertEquals(0, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		QVehicle v = new QVehicle(f.basicVehicle);

		PersonImpl p = new PersonImpl(new IdImpl("1"));
		p.addPlan(new PlanImpl());
		v.setDriver(createAndInsertPersonDriverAgentImpl(p, f.sim));

		f.qlink1.addFromUpstream(v);
		assertEquals(1, ((QueueWithBuffer) f.qlink1.road).vehInQueueCount());
		assertFalse(f.qlink1.isAcceptingFromUpstream());
		assertTrue(f.qlink1.isNotOfferingVehicle());
	}


	private PersonDriverAgentImpl createAndInsertPersonDriverAgentImpl(PersonImpl p, QSim simulation) {
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
		Id id1 = new IdImpl("1");

		QVehicle veh = new QVehicle(f.basicVehicle);
		PersonImpl p = new PersonImpl(new IdImpl(23));
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
		Id id1 = new IdImpl("1");

		QVehicle veh = new QVehicle(f.basicVehicle);
		PersonImpl p = new PersonImpl(new IdImpl(42));
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
		Id id1 = new IdImpl("1");


		QVehicle veh = new QVehicle(f.basicVehicle);
		PersonImpl pers = new PersonImpl(new IdImpl(80));
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
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(conf);
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(1.0);
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2, 0));
		Link link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 1.0, 1.0);
		Link link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 1.0, 1.0, 1.0, 1.0);
		QSim qsim = (QSim) new QSimFactory().createMobsim(scenario, (EventsUtils.createEventsManager()));
		NetsimNetwork queueNetwork = qsim.getNetsimNetwork();
		QLinkImpl qlink = (QLinkImpl) queueNetwork.getNetsimLink(new IdImpl("1"));

		QVehicle v1 = new QVehicle(new VehicleImpl(new IdImpl("1"), new VehicleTypeImpl(new IdImpl("defaultVehicleType"))));
		PersonImpl p = new PersonImpl(new IdImpl("1"));
		PlanImpl plan = p.createAndAddPlan(true);
		plan.createAndAddActivity("h", link1.getId());
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) ((PopulationFactoryImpl) scenario.getPopulation().getFactory()).createRoute(TransportMode.car, link1.getId(), link2.getId());
		leg.setRoute(route);
		route.setLinkIds(link1.getId(), null, link2.getId());
		leg.setRoute(route);
		plan.createAndAddActivity("w", link2.getId());
		PersonDriverAgentImpl pa1 = createAndInsertPersonDriverAgentImpl(p, qsim);
		v1.setDriver(pa1);
		pa1.setVehicle(v1);
		pa1.endActivityAndComputeNextState(0);

		QVehicle v2 = new QVehicle(new VehicleImpl(new IdImpl("2"), new VehicleTypeImpl(new IdImpl("defaultVehicleType"))));
		PersonDriverAgentImpl pa2 = createAndInsertPersonDriverAgentImpl(p, qsim);
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

	public void testStorageSpaceDifferentVehicleSizes() {
		Fixture f = new Fixture();
		PersonImpl p = new PersonImpl(new IdImpl(5));
		Plan plan = new PlanImpl();
		p.addPlan(plan);
		plan.addActivity(new ActivityImpl("home", f.link1.getId()));
		Leg leg = new LegImpl(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.link1.getId(), f.link2.getId());
		route.setVehicleId(f.basicVehicle.getId());
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", f.link2.getId()));

		VehicleType defaultVehType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		VehicleType mediumVehType = new VehicleTypeImpl(new IdImpl("mediumVehicleType"));
		mediumVehType.setPcuEquivalents(2.5);
		VehicleType largeVehType = new VehicleTypeImpl(new IdImpl("largeVehicleType"));
		largeVehType.setPcuEquivalents(5);
		QVehicle veh1 = new QVehicle(new VehicleImpl(new IdImpl(1), defaultVehType));
		PersonDriverAgentImpl driver1 = createAndInsertPersonDriverAgentImpl(p, f.sim);
		veh1.setDriver(driver1);
		driver1.setVehicle(veh1);
		driver1.endActivityAndComputeNextState(0.0);
		QVehicle veh25 = new QVehicle(new VehicleImpl(new IdImpl(2), mediumVehType));
		PersonDriverAgentImpl driver25 = createAndInsertPersonDriverAgentImpl(p, f.sim);
		veh25.setDriver(driver25);
		driver25.setVehicle(veh25);
		driver25.endActivityAndComputeNextState(0.0);
		QVehicle veh5 = new QVehicle(new VehicleImpl(new IdImpl(3), largeVehType));
		PersonDriverAgentImpl driver5 = createAndInsertPersonDriverAgentImpl(p, f.sim);
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

	public void testStuckEvents() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		scenario.getConfig().qsim().setStuckTime(100);
		scenario.getConfig().qsim().setRemoveStuckVehicles(true);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1001, 0));
		Link link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 3600.0, 1.0);
		Link link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 10 * 7.5, 7.5, 3600.0, 1.0);
		Link link3 = network.createAndAddLink(new IdImpl("3"), node2, node2, 2 * 7.5, 7.5, 3600.0, 1.0);

		for (int i = 0; i < 5; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i));
			PlanImpl plan = new PlanImpl();
			Activity act = new ActivityImpl("h", link1.getId());
			act.setEndTime(7*3600);
			plan.addActivity(act);
			Leg leg = new LegImpl("car");
			NetworkRoute route = new LinkNetworkRouteImpl(link1.getId(), link2.getId());
			List<Id> links = new ArrayList<Id>();
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

		QSim sim = (QSim) new QSimFactory().createMobsim(scenario, EventsUtils.createEventsManager());

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
		/*package*/ final ScenarioImpl scenario;
		/*package*/ final Link link1;
		/*package*/ final Link link2;
		/*package*/ final QNetwork queueNetwork;
		/*package*/ final QLinkImpl qlink1;
		/*package*/ final QLinkImpl qlink2;
		/*package*/ final Vehicle basicVehicle;
		/*package*/ final QSim sim;

		/*package*/ Fixture() {
			this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			this.scenario.getConfig().qsim().setStuckTime(100);
			this.scenario.getConfig().qsim().setRemoveStuckVehicles(true);
			NetworkImpl network = (NetworkImpl) this.scenario.getNetwork();
			network.setCapacityPeriod(3600.0);
			Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
			Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1, 0));
			Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1001, 0));
			this.link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 3600.0, 1.0);
			this.link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 10 * 7.5, 2.0 * 7.5, 3600.0, 1.0);
			sim = (QSim) new QSimFactory().createMobsim(scenario, (EventsUtils.createEventsManager()));
			this.queueNetwork = (QNetwork) sim.getNetsimNetwork();
			this.qlink1 = (QLinkImpl) this.queueNetwork.getNetsimLink(new IdImpl("1"));
			this.qlink2 = (QLinkImpl) this.queueNetwork.getNetsimLink(new IdImpl("2"));

			this.basicVehicle = new VehicleImpl(new IdImpl("1"), new VehicleTypeImpl(new IdImpl("defaultVehicleType")));
		}

	}

}
