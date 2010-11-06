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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PersonDriverAgentImpl;
import org.matsim.ptproject.qsim.interfaces.NetsimLink;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.qnetsimengine.QLinkImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * @author dgrether
 * @author mrieser
 */
public class QLinkTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

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
		assertEquals(f.queueNetwork.getNetsimNode(new IdImpl("2")), f.qlink1.getToQueueNode());
	}


	public void testAdd() {
		Fixture f = new Fixture();
		assertEquals(0, f.qlink1.vehOnLinkCount());
		QVehicleImpl v = new QVehicleImpl(f.basicVehicle);

		PersonImpl p = new PersonImpl(new IdImpl("1"));
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		v.setDriver(new PersonDriverAgentImpl(p, new QSim(scenario, new EventsManagerImpl())));

		f.qlink1.addFromIntersection(v);
		assertEquals(1, f.qlink1.vehOnLinkCount());
		assertFalse(f.qlink1.hasSpace());
		assertTrue(f.qlink1.bufferIsEmpty());
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

		QSim qsim = new QSim(f.scenario, new EventsManagerImpl());

		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		PersonImpl p = new PersonImpl(new IdImpl(23));
		veh.setDriver(new PersonDriverAgentImpl(p, qsim));

		// start test, check initial conditions
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertNull(f.qlink1.getVehicle(id1));
		assertEquals(0, f.qlink1.getAllVehicles().size());

		// add a vehicle, it should be now in the vehicle queue
		f.qlink1.addFromIntersection(veh);
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(1, f.qlink1.vehOnLinkCount());
		assertEquals("vehicle not found on link.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());

		// time step 1, vehicle should be now in the buffer
		f.qlink1.moveLink(1.0);
		assertFalse(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		// time step 2, vehicle leaves link
		f.qlink1.moveLink(2.0);
		assertEquals(veh, f.qlink1.popFirstFromBuffer());
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
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

		QSim qsim = new QSim(f.scenario, new EventsManagerImpl());

		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		PersonImpl p = new PersonImpl(new IdImpl(42));
		veh.setDriver(new PersonDriverAgentImpl(p, qsim));

		// start test, check initial conditions
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals(0, f.qlink1.getAllVehicles().size());

		f.qlink1.addParkedVehicle(veh);
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals("vehicle not found in parking list.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		assertEquals("removed wrong vehicle.", veh, f.qlink1.removeParkedVehicle(veh.getId()));
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
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

		QSim qsim = new QSim(f.scenario, new EventsManagerImpl());

		QVehicle veh = new QVehicleImpl(f.basicVehicle);
		PersonImpl pers = new PersonImpl(new IdImpl(80));
		Plan plan = new PlanImpl();
		pers.addPlan(plan);
		plan.addActivity(new ActivityImpl("home", f.link1.getId()));
		Leg leg = new LegImpl(TransportMode.car);
		leg.setRoute(new LinkNetworkRouteImpl(f.link1.getId(), f.link2.getId()));
		plan.addLeg(leg);
		plan.addActivity(new ActivityImpl("work", f.link2.getId()));
		PersonDriverAgentImpl driver = new PersonDriverAgentImpl(pers, qsim);
		driver.initializeAndCheckIfAlive();
		veh.setDriver(driver);
		driver.setVehicle(veh);
		driver.endActivityAndAssumeControl(0);

		// start test, check initial conditions
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals(0, f.qlink1.getAllVehicles().size());

		f.qlink1.addDepartingVehicle(veh);
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals("vehicle not found in waiting list.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(veh, f.qlink1.getAllVehicles().iterator().next());

		// time step 1, vehicle should be now in the buffer
		f.qlink1.moveLink(1.0);
		assertFalse(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
		assertEquals("vehicle not found in buffer.", veh, f.qlink1.getVehicle(id1));
		assertEquals(1, f.qlink1.getAllVehicles().size());

		// vehicle leaves link
		assertEquals(veh, f.qlink1.popFirstFromBuffer());
		assertTrue(f.qlink1.bufferIsEmpty());
		assertEquals(0, f.qlink1.vehOnLinkCount());
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
	  ScenarioImpl scenario = new ScenarioImpl(conf);
	  conf.setQSimConfigGroup(new QSimConfigGroup());

		NetworkImpl network = scenario.getNetwork();
		network.setCapacityPeriod(1.0);
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2, 0));
		Link link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 1.0, 1.0);
		Link link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 1.0, 1.0, 1.0, 1.0);
		QSim qsim = new QSim(scenario, new EventsManagerImpl());
		NetsimNetwork queueNetwork = qsim.getNetsimNetwork();
		QLinkImpl qlink = (QLinkImpl) queueNetwork.getNetsimLink(new IdImpl("1"));

		QVehicleImpl v1 = new QVehicleImpl(new VehicleImpl(new IdImpl("1"), new VehicleTypeImpl(new IdImpl("defaultVehicleType"))));
		PersonImpl p = new PersonImpl(new IdImpl("1"));
		PlanImpl plan = p.createAndAddPlan(true);
		try {
			plan.createAndAddActivity("h", link1.getId());
			LegImpl leg = plan.createAndAddLeg(TransportMode.car);
			NetworkRoute route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, link1.getId(), link2.getId());
			leg.setRoute(route);
			route.setLinkIds(link1.getId(), null, link2.getId());
			leg.setRoute(route);
			plan.createAndAddActivity("w", link2.getId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PersonDriverAgentImpl pa1 = new PersonDriverAgentImpl(p, qsim);
		v1.setDriver(pa1);
		pa1.setVehicle(v1);
		pa1.initializeAndCheckIfAlive();

		QVehicleImpl v2 = new QVehicleImpl(new VehicleImpl(new IdImpl("2"), new VehicleTypeImpl(new IdImpl("defaultVehicleType"))));
		PersonDriverAgentImpl pa2 = new PersonDriverAgentImpl(p, qsim);
		v2.setDriver(pa2);
		pa2.setVehicle(v2);
		pa2.initializeAndCheckIfAlive();

		// start test
		assertTrue(qlink.bufferIsEmpty());
		assertEquals(0, qlink.vehOnLinkCount());
		// add v1
		qlink.addFromIntersection(v1);
		assertEquals(1, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 1, v1 is moved to buffer
		qlink.moveLink(1.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// add v2, still time step 1
		qlink.addFromIntersection(v2);
		assertEquals(1, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// time step 2, v1 still in buffer, v2 cannot enter buffer, so still on link
		qlink.moveLink(2.0);
		assertEquals(1, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// v1 leaves buffer
		assertEquals(v1, qlink.popFirstFromBuffer());
		assertEquals(1, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 3, v2 moves to buffer
		qlink.moveLink(3.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertFalse(qlink.bufferIsEmpty());
		// v2 leaves buffer
		assertEquals(v2, qlink.popFirstFromBuffer());
		assertEquals(0, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
		// time step 4, empty link
		qlink.moveLink(4.0);
		assertEquals(0, qlink.vehOnLinkCount());
		assertTrue(qlink.bufferIsEmpty());
	}

	public void testStorageSpaceDifferentVehicleSizes() {
		Fixture f = new Fixture();
		PersonImpl p = new PersonImpl(new IdImpl(5));
		QSim qsim = new QSim(f.scenario, new EventsManagerImpl());

		VehicleType vehType = new VehicleTypeImpl(new IdImpl("defaultVehicleType"));
		QVehicle veh1 = new QVehicleImpl(new VehicleImpl(new IdImpl(1), vehType));
		veh1.setDriver(new PersonDriverAgentImpl(p, qsim));
		QVehicle veh25 = new QVehicleImpl(new VehicleImpl(new IdImpl(2), vehType), 2.5);
		veh25.setDriver(new PersonDriverAgentImpl(p, null));
		QVehicle veh5 = new QVehicleImpl(new VehicleImpl(new IdImpl(3), vehType), 5);
		veh5.setDriver(new PersonDriverAgentImpl(p, null));

		assertEquals("wrong initial storage capacity.", 10.0, f.qlink2.getSpaceCap(), EPSILON);
		f.qlink2.addFromIntersection(veh5);  // used vehicle equivalents: 5
		assertTrue(f.qlink2.hasSpace());
		f.qlink2.addFromIntersection(veh5);  // used vehicle equivalents: 10
		assertFalse(f.qlink2.hasSpace());

		assertTrue(f.qlink2.bufferIsEmpty());
		f.qlink2.moveLink(5.0); // first veh moves to buffer, used vehicle equivalents: 5
		assertTrue(f.qlink2.hasSpace());
		assertFalse(f.qlink2.bufferIsEmpty());
		f.qlink2.popFirstFromBuffer();  // first veh leaves buffer
		assertTrue(f.qlink2.hasSpace());

		f.qlink2.addFromIntersection(veh25); // used vehicle equivalents: 7.5
		f.qlink2.addFromIntersection(veh1);  // used vehicle equivalents: 8.5
		f.qlink2.addFromIntersection(veh1);  // used vehicle equivalents: 9.5
		assertTrue(f.qlink2.hasSpace());
		f.qlink2.addFromIntersection(veh1);  // used vehicle equivalents: 10.5
		assertFalse(f.qlink2.hasSpace());

		f.qlink2.moveLink(6.0); // first veh moves to buffer, used vehicle equivalents: 5.5
		assertTrue(f.qlink2.hasSpace());
		f.qlink2.addFromIntersection(veh1);  // used vehicle equivalents: 6.5
		f.qlink2.addFromIntersection(veh25); // used vehicle equivalents: 9.0
		f.qlink2.addFromIntersection(veh1);  // used vehicle equivalents: 10.0
		assertFalse(f.qlink2.hasSpace());

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
		/*package*/ final NetsimNetwork queueNetwork;
		/*package*/ final QLinkImpl qlink1;
		/*package*/ final QLinkImpl qlink2;
		/*package*/ final Vehicle basicVehicle;

		/*package*/ Fixture() {
			this.scenario = new ScenarioImpl();
			this.scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
			NetworkImpl network = this.scenario.getNetwork();
			network.setCapacityPeriod(3600.0);
			Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
			Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1, 0));
			Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1001, 0));
			this.link1 = network.createAndAddLink(new IdImpl("1"), node1, node2, 1.0, 1.0, 3600.0, 1.0);
			this.link2 = network.createAndAddLink(new IdImpl("2"), node2, node3, 10 * 7.5, 2.0 * 7.5, 3600.0, 1.0);
			QSim sim = new QSim(scenario, new EventsManagerImpl());
			this.queueNetwork = sim.getNetsimNetwork();
			this.qlink1 = (QLinkImpl) this.queueNetwork.getNetsimLink(new IdImpl("1"));
			this.qlink2 = (QLinkImpl) this.queueNetwork.getNetsimLink(new IdImpl("2"));

			this.basicVehicle = new VehicleImpl(new IdImpl("1"), new VehicleTypeImpl(new IdImpl("defaultVehicleType")));
		}

	}

}
