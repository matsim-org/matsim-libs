/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueNetworkTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.qsim;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.fakes.FakeAgent;
import org.matsim.ptproject.qsim.QPersonAgent;
import org.matsim.ptproject.qsim.QLinkImpl;
import org.matsim.ptproject.qsim.QNetwork;
import org.matsim.ptproject.qsim.QSimEngine;
import org.matsim.ptproject.qsim.QSimTimer;
import org.matsim.ptproject.qsim.QVehicle;
import org.matsim.ptproject.qsim.QVehicleImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicleTypeImpl;


public class TransitQueueNetworkTest extends TestCase {

	/**
	 * Tests that a non-blocking stops on the first link of a transit vehicle's network
	 * route is correctly handled.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testNonBlockingStop_FirstLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(1, false, 0, false);

		f.simEngine.simStep(100);
		assertEquals(2, f.qlink1.getAllVehicles().size());
		assertEquals(0, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(101);
    assertEquals(1, f.qlink1.getAllVehicles().size());
    assertEquals(1, f.qlink2.getAllVehicles().size());
		f.simEngine.simStep(102);
		Collection<QVehicle> allVehicles = f.qlink2.getAllVehicles();
		assertEquals(1, allVehicles.size());
		assertEquals(f.normalVehicle, f.qlink2.getAllVehicles().toArray(new QVehicle[1])[0]); // first the normal vehicle

		f.simEngine.simStep(103);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(119);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(120); // 100 (departure) + 19 (stop delay) + 1 (buffer2node)
		assertEquals(2, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]); // second the transit vehicle

		f.simEngine.simStep(117);
		assertEquals(2, f.qlink2.getAllVehicles().size());
	}

	/**
	 * Tests that blocking stops are correctly handled on the
	 * first link of a transit vehicle's network route.
	 * Note that on the first link, a stop is by definition non-blocking,
	 * as the wait2buffer-queue is seen as similary independent than the transit stop queue!
	 * So, it essentially tests the same thing as {@link #testNonBlockingStop_FirstLink()}.
	 *
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testBlockingStop_FirstLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(1, true, 0, false);

		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(f.normalVehicle, f.qlink2.getAllVehicles().toArray(new QVehicle[1])[0]); // first the normal vehicle

		f.simEngine.simStep(102);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(119);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		f.simEngine.simStep(120); // 100 (departure) + 19 (stop delay) + 1 (buffer2node)
		assertEquals(2, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]); // second the transit vehicle

		f.simEngine.simStep(117);
		assertEquals(2, f.qlink2.getAllVehicles().size());
	}

	/**
	 * Tests that a non-blocking stop is correctly handled when it is somewhere in the middle
	 * of the transit vehicle's network route.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testNonBlockingStop_MiddleLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(2, false, 0, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 200: both vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is moved to qlink2.transitStopQueue (delay=19, exit-time 220)
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is at stop, normalVeh moved to qlink2.buffer
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 203: normalVeh moved to qlink3
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 204: transitVeh still at qlink2.stop, normalVeh on qlink3
		QSimTimer.setTime(204);
		f.simEngine.simStep(204);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 219: nothing changed since 204
		QSimTimer.setTime(219);
		f.simEngine.simStep(219);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 220: transitVeh moves now to qlink2.buffer
		QSimTimer.setTime(220);
		f.simEngine.simStep(220);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 221: transitVeh moves finally to qlink3
		QSimTimer.setTime(221);
		f.simEngine.simStep(221);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);
	}

	/**
	 * Tests that a blocking stop is correctly handled when it is somewhere in the middle
	 * of the transit vehicle's network route.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testBlockingStop_MiddleLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(2, true, 0, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 200: both vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is blocking qlink2 now (delay=19, exit-time 220)
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is at stop, normalVeh has to wait
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 203: normalVeh cannot move to qlink3
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 219: nothing changed since 203
		QSimTimer.setTime(219);
		f.simEngine.simStep(219);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 220: transitVeh moves now to qlink2.buffer
		QSimTimer.setTime(220);
		f.simEngine.simStep(220);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 221: transitVeh moves finally to qlink3, normalVeh is moved to qlink2.buffer
		QSimTimer.setTime(221);
		f.simEngine.simStep(221);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 222: normalVeh also moves to qlink3
		QSimTimer.setTime(222);
		f.simEngine.simStep(222);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);
	}

	/**
	 * Tests that a non-blocking stop is correctly handled when it is the last link
	 * of the transit vehicle's network route.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testNonBlockingStop_LastLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(3, false, 0, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 201: transitVeh is moved to qlink2.buffer
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is moved to qlink3 (exit-time: 302), normalVeh moved to qlink2.buffer
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 203: normalVeh is moved to qlink3 (exit-time: 303)
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 302: transitVeh is moved to qlink3.transitStopQueue (delay: 19, exit-time: 321)
		QSimTimer.setTime(302);
		f.simEngine.simStep(302);
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 303: normalVeh leaves qlink3, respectively parks on qlink3
		QSimTimer.setTime(303);
		f.simEngine.simStep(303);
		assertEquals(2, f.qlink3.getAllVehicles().size()); // includes parked vehicles
		assertEquals(1, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 320: transitVeh is still at the stop
		QSimTimer.setTime(320);
		f.simEngine.simStep(320);
		assertEquals(2, f.qlink3.getAllVehicles().size()); // includes parked vehicles
		assertEquals(1, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 321: transitVeh leaves stop and link
		QSimTimer.setTime(321);
		f.simEngine.simStep(321);
		assertEquals(2, f.qlink3.getAllVehicles().size()); // includes parked vehicles
		assertEquals(0, f.qlink3.getAllNonParkedVehicles().size());
	}

	/**
	 * Tests that a blocking stop is correctly handled when it is the last link
	 * of the transit vehicle's network route.
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public void testBlockingStop_LastLink() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Fixture f = new Fixture(3, true, 0, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 201: transitVeh is moved to qlink2.buffer
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is moved to qlink3 (exit-time: 302), normalVeh moved to qlink2.buffer
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 203: normalVeh is moved to qlink3 (exit-time: 303)
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 302: transitVeh is at stop (delay: 19, exit-time: 321)
		QSimTimer.setTime(302);
		f.simEngine.simStep(302);
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 303: transitVeh blocks normalVeh
		QSimTimer.setTime(303);
		f.simEngine.simStep(303);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 321: transitVeh leaves stop and link, also normalVeh leaves link
		QSimTimer.setTime(321);
		f.simEngine.simStep(321);
		assertEquals(2, f.qlink3.getAllVehicles().size()); // includes parked vehicles
		assertEquals(0, f.qlink3.getAllNonParkedVehicles().size());
	}

	public void testTwoStopsOnOneLink_FirstLink() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Fixture f = new Fixture(1, true, 1, true); // first stop at the first link is non-blocking by definition!

		// time 100: agents start, transitVeh is moved to qlink1.transitStopQueue (delay 19, exit-time 119), normalVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is *not* blocking qlink1, normalVeh is moved to qlink2
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 119: transitVeh is moved to qlink2.vehQueue (stop2, blocking, delay 19, exit-time 138)
		QSimTimer.setTime(119);
		f.simEngine.simStep(119);
		assertEquals(1, f.qlink2.getAllVehicles().size());

		// time 120: normalVeh2 departs, cannot be blocked from waitingQueue, so moved to buffer
		QSimTimer.setTime(120);
		f.normalVehicle2.getDriver().activityEnds(120);
		f.simEngine.simStep(120);

		// time 121: normalVeh2 moves to qlink2 (exit-time 221)
		QSimTimer.setTime(121);
		f.simEngine.simStep(121);
		assertEquals(1, f.qlink1.getAllVehicles().size());
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 138: transitVeh moved to qlink1.buffer
		QSimTimer.setTime(138);
		f.simEngine.simStep(138);
		assertEquals(1, f.qlink1.getAllVehicles().size());

		// time 139: transitVeh is moved to qlink2
		QSimTimer.setTime(139);
		f.simEngine.simStep(139);
		assertEquals(3, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);
		assertEquals(f.transitVehicle, vehicles[2]);
	}

	public void testTwoStopsOnOneLink_MiddleLink_FirstBlockThenNonBlock() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Fixture f = new Fixture(2, true, 2, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 120: normalVeh2 departs, moved to buffer
		QSimTimer.setTime(124);
		f.normalVehicle2.getDriver().activityEnds(124);
		f.simEngine.simStep(124);

		// time 125: normalVeh2 moves to qlink2 (exit-time 225)
		QSimTimer.setTime(125);
		f.simEngine.simStep(125);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 200: all vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is blocking qlink2 (stop1, delay=19, exit-time 220)
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is at stop1, normalVeh must wait, normalVeh2 still driving
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 219: transitVeh is at stop1, normalVeh must wait
		QSimTimer.setTime(219);
		f.simEngine.simStep(219);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 220: transitVeh moved from stop1 (blocking) to stop2 (non-blocking, delay 19, exit-time 239), normalVeh moved to qlink2.buffer
		QSimTimer.setTime(220);
		f.simEngine.simStep(220);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 221: transitVeh at stop2, normalVeh moved to qlink3, normalVeh2 still on qlink2
		QSimTimer.setTime(221);
		f.simEngine.simStep(221);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 239: transitVeh moved to qlink2.buffer, normalVeh2 still waiting behind transitVeh
		QSimTimer.setTime(239);
		f.simEngine.simStep(239);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle2, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 240: transitVeh moved to qlink3
		QSimTimer.setTime(240);
		f.simEngine.simStep(240);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle2, vehicles[0]);
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);

		// time 241: normalVeh2 moved to qlink3
		QSimTimer.setTime(241);
		f.simEngine.simStep(241);
		assertEquals(0, f.qlink2.getAllVehicles().size());
		assertEquals(3, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);
		assertEquals(f.normalVehicle2, vehicles[2]);
	}

	public void testTwoStopsOnOneLink_MiddleLink_FirstNonBlockThenBlock() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Fixture f = new Fixture(2, false, 2, true);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 124: normalVeh2 departs, moved to buffer
		QSimTimer.setTime(124);
		f.normalVehicle2.getDriver().activityEnds(124);
		f.simEngine.simStep(124);

		// time 125: normalVeh2 moves to qlink2 (exit-time 225)
		QSimTimer.setTime(125);
		f.simEngine.simStep(125);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 200: all vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is blocking qlink2 (stop1, delay=19, exit-time 220)
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh is at stop1, normalVeh must wait
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 219: transitVeh is at stop1, normalVeh must wait
		QSimTimer.setTime(219);
		f.simEngine.simStep(219);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 220: transitVeh moved from stop1 (blocking) to stop2 (non-blocking, delay 19, exit-time 239), normalVeh moved to qlink2.buffer
		QSimTimer.setTime(220);
		f.simEngine.simStep(220);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 221: transitVeh at stop2, normalVeh moved to qlink3
		QSimTimer.setTime(221);
		f.simEngine.simStep(221);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 225: transitVeh at stop2, normalVeh on qlink3, normalVeh2 is blocked
		QSimTimer.setTime(225);
		f.simEngine.simStep(225);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 226: transitVeh at stop2, normalVeh2 is blocked, normalVeh on qlink3
		QSimTimer.setTime(226);
		f.simEngine.simStep(226);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);

		// time 239: transitVeh moved to qlink2.buffer
		QSimTimer.setTime(239);
		f.simEngine.simStep(239);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle2, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 240: transitVeh moved to qlink3, normalVeh2 moved to qlink2.buffer
		QSimTimer.setTime(240);
		f.simEngine.simStep(240);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);

		// time 241: normalVeh2 moved to qlink3
		QSimTimer.setTime(241);
		f.simEngine.simStep(241);
		assertEquals(0, f.qlink2.getAllVehicles().size());
		assertEquals(3, f.qlink3.getAllVehicles().size());
		vehicles = f.qlink3.getAllVehicles().toArray(vehicles);
		assertEquals(f.normalVehicle, vehicles[0]);
		assertEquals(f.transitVehicle, vehicles[1]);
		assertEquals(f.normalVehicle2, vehicles[2]);
	}

	public void testTwoStopsOnOneLink_LastLink_FirstBlockThenNonBlock() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Fixture f = new Fixture(3, true, 3, false);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 124: normalVeh2 departs, moved to qlink1.buffer
		QSimTimer.setTime(124);
		f.normalVehicle2.getDriver().activityEnds(124);
		f.simEngine.simStep(124);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 125: normalVeh2 moves to qlink2 (exit-time 225)
		QSimTimer.setTime(125);
		f.simEngine.simStep(125);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 200: all vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is moved to qlink2.buffer
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh @ qlink3 (exit-time 302), normalVeh @ qlink2.buffer
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 203: transitVeh @ qlink3, normalVeh @ qlink3 (exit-time 303)
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 225: transitVeh @ qlink3, normalVeh @ qlink3, normalVeh2 @ qlink2.buffer
		QSimTimer.setTime(225);
		f.simEngine.simStep(225);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 226: transitVeh @ qlink3, normalVeh @ qlink3, normalVeh2 @ qlink3 (exit-time 326)
		QSimTimer.setTime(226);
		f.simEngine.simStep(226);
		assertEquals(3, f.qlink3.getAllVehicles().size());

		// time 302: transitVeh @ stop2 (blocking, delay 19, exit-time 321)
		QSimTimer.setTime(302);
		f.simEngine.simStep(302);
		assertEquals(3, f.qlink3.getAllVehicles().size());

		// time 320: transitVeh @ stop2, normalVeh @ qlink3 (blocked)
		QSimTimer.setTime(320);
		f.simEngine.simStep(320);
		assertEquals(3, f.qlink3.getAllVehicles().size());

		// time 321: transitVeh @ stop3 (non-blocking, delay 19, exit-time 340), normalVeh left qlink3
		QSimTimer.setTime(321);
		f.simEngine.simStep(321);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 325: transitVeh @ stop3, normalVeh2 @ qlink3
		QSimTimer.setTime(325);
		f.simEngine.simStep(325);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 326: transitVeh @ stop3, normalVeh2 left qlink3
		QSimTimer.setTime(326);
		f.simEngine.simStep(326);
		assertEquals(1, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 339: transitVeh @ stop3
		QSimTimer.setTime(339);
		f.simEngine.simStep(339);
		assertEquals(1, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 340: transitVeh left qlink3
		QSimTimer.setTime(340);
		f.simEngine.simStep(340);
		assertEquals(0, f.qlink3.getAllNonParkedVehicles().size());
	}

	public void testTwoStopsOnOneLink_LastLink_FirstNonBlockThenBlock() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Fixture f = new Fixture(3, false, 3, true);

		// time 100: agents start, transitVeh is moved to qlink1.buffer
		f.simEngine.simStep(100);
		assertEquals(0, f.qlink2.getAllVehicles().size());

		// time 101: transitVeh is moved to qlink2 (exit-time=201), normalVeh is moved to qlink1.buffer
		QSimTimer.setTime(101);
		f.simEngine.simStep(101);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		QVehicle[] vehicles = f.qlink2.getAllVehicles().toArray(new QVehicle[2]);
		assertEquals(f.transitVehicle, vehicles[0]);

		// time 102: normalVeh is moved to qlink2 (exit-time=202)
		QSimTimer.setTime(102);
		f.simEngine.simStep(102);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		vehicles = f.qlink2.getAllVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle, vehicles[1]);

		// time 120: normalVeh2 departs, moved to qlink1.buffer
		QSimTimer.setTime(120);
		f.normalVehicle2.getDriver().activityEnds(120);
		f.simEngine.simStep(120);
		assertEquals(2, f.qlink2.getAllVehicles().size());

		// time 121: normalVeh2 moves to qlink2 (exit-time 221)
		QSimTimer.setTime(121);
		f.simEngine.simStep(121);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 200: all vehicles still on qlink2
		QSimTimer.setTime(200);
		f.simEngine.simStep(200);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 201: transitVeh is moved to qlink2.buffer
		QSimTimer.setTime(201);
		f.simEngine.simStep(201);
		assertEquals(3, f.qlink2.getAllVehicles().size());

		// time 202: transitVeh @ qlink3 (exit-time 302), normalVeh @ qlink2.buffer
		QSimTimer.setTime(202);
		f.simEngine.simStep(202);
		assertEquals(2, f.qlink2.getAllVehicles().size());
		assertEquals(1, f.qlink3.getAllVehicles().size());

		// time 203: transitVeh @ qlink3, normalVeh @ qlink3 (exit-time 303)
		QSimTimer.setTime(203);
		f.simEngine.simStep(203);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 221: transitVeh @ qlink3, normalVeh @ qlink3, normalVeh2 @ qlink2.buffer
		QSimTimer.setTime(221);
		f.simEngine.simStep(221);
		assertEquals(1, f.qlink2.getAllVehicles().size());
		assertEquals(2, f.qlink3.getAllVehicles().size());

		// time 222: transitVeh @ qlink3, normalVeh @ qlink3, normalVeh2 @ qlink3 (exit-time 322)
		QSimTimer.setTime(222);
		f.simEngine.simStep(222);
		assertEquals(3, f.qlink3.getAllVehicles().size());

		// time 302: transitVeh @ stop2 (non-blocking, delay 19, exit-time 321)
		QSimTimer.setTime(302);
		f.simEngine.simStep(302);
		assertEquals(3, f.qlink3.getAllVehicles().size());

		// time 303: transitVeh @ stop2, normalVeh left qlink3
		QSimTimer.setTime(303);
		f.simEngine.simStep(303);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 320: transitVeh @ stop2
		QSimTimer.setTime(320);
		f.simEngine.simStep(320);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());

		// time 321: transitVeh @ stop3 (blocking, delay 19, exit-time 340)
		QSimTimer.setTime(321);
		f.simEngine.simStep(321);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 339: transitVeh @ stop3, normalVeh2 blocked behind transitVeh
		QSimTimer.setTime(339);
		f.simEngine.simStep(339);
		assertEquals(2, f.qlink3.getAllNonParkedVehicles().size());
		vehicles = f.qlink3.getAllNonParkedVehicles().toArray(vehicles);
		assertEquals(f.transitVehicle, vehicles[0]);
		assertEquals(f.normalVehicle2, vehicles[1]);

		// time 340: transitVeh left qlink3, and also normalVeh2 left qlink3 (no flow-restriction when leaving link)
		QSimTimer.setTime(340);
		f.simEngine.simStep(340);
		assertEquals(0, f.qlink3.getAllNonParkedVehicles().size());
	}

//	protected static class TestSimEngine extends QSimEngineImpl {
//		TestSimEngine(final QSim sim) {
//			super(sim, new Random(511));
//		}
//	}

	protected static class Fixture {
		public final QSimEngine simEngine;
		public final QLinkImpl qlink1, qlink2, qlink3;
		public final TransitQVehicle transitVehicle;
		public final QVehicle normalVehicle, normalVehicle2;

		/**
		 * @param firstStopLocation
		 * @param firstStopisBlocking
		 * @param secondStopLocation if 0, no second stop will be created
		 * @param secondStopIsBlocking
		 * @throws IllegalArgumentException
		 * @throws IllegalAccessException
		 * @throws InvocationTargetException
		 * @throws SecurityException
		 * @throws NoSuchMethodException
		 */
		public Fixture(final int firstStopLocation, final boolean firstStopisBlocking, final int secondStopLocation, final boolean secondStopIsBlocking)
				throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
			// setup: config
			ScenarioImpl scenario = new ScenarioImpl();
			scenario.getConfig().scenario().setUseTransit(true);
			scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
			Id id1 = scenario.createId("1");
			Id id2 = scenario.createId("2");
			Id id3 = scenario.createId("3");
			Id id4 = scenario.createId("4");

			// setup: network
			NetworkLayer network = scenario.getNetwork();
			Node node1 = network.createAndAddNode(id1, scenario.createCoord(   0, 0));
			Node node2 = network.createAndAddNode(id2, scenario.createCoord(1000, 0));
			Node node3 = network.createAndAddNode(id3, scenario.createCoord(2000, 0));
			Node node4 = network.createAndAddNode(id4, scenario.createCoord(3000, 0));
			Link[] links = new Link[4];
			links[1] = network.createAndAddLink(id1, node1, node2, 1000.0, 10.0, 3600.0, 1);
			links[2] = network.createAndAddLink(id2, node2, node3, 1000.0, 10.0, 3600.0, 1);
			links[3] = network.createAndAddLink(id3, node3, node4, 1000.0, 10.0, 3600.0, 1);

			// setup: population
			Population population = scenario.getPopulation();
			PopulationFactory pb = population.getFactory();
			Person person = pb.createPerson(id2);
			Plan plan = pb.createPlan();
			person.addPlan(plan);
			Activity act = pb.createActivityFromLinkId("home", id1);
			plan.addActivity(act);
			Leg leg = pb.createLeg(TransportMode.car);
			LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(links[1].getId(), links[3].getId());
			List<Id> linkIds_2 = new ArrayList<Id>();
			linkIds_2.add(links[2].getId());
			route.setLinkIds(links[1].getId(), linkIds_2, links[3].getId());
			leg.setRoute(route);
			plan.addLeg(leg);
			plan.addActivity(pb.createActivityFromLinkId("work", id2));
			population.addPerson(person);

			// setup: transit schedule
			TransitSchedule schedule = scenario.getTransitSchedule();
			TransitScheduleFactory builder = schedule.getFactory();
			TransitStopFacility stop1 = builder.createTransitStopFacility(id1, scenario.createCoord(0, 0), firstStopisBlocking);
			schedule.addStopFacility(stop1);
			stop1.setLinkId(links[firstStopLocation].getId());
			TransitStopFacility stop2 = null;
			if (secondStopLocation > 0) {
				stop2 = builder.createTransitStopFacility(id2, scenario.createCoord(100, 0), secondStopIsBlocking);
				schedule.addStopFacility(stop2);
				stop2.setLinkId(links[secondStopLocation].getId());
			}
			TransitLine tLine = builder.createTransitLine(id1);
			NetworkRoute netRoute = new LinkNetworkRouteImpl(links[1].getId(), links[3].getId());
			netRoute.setLinkIds(links[1].getId(), linkIds_2, links[3].getId());
			ArrayList<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
			stops.add(builder.createTransitRouteStop(stop1, 50, 60));
			if (stop2 != null) {
				stops.add(builder.createTransitRouteStop(stop2, 70, 80));
			}
			TransitRoute tRoute = builder.createTransitRoute(id1, netRoute, stops, TransportMode.pt);
			Departure dep = builder.createDeparture(id1, 100);

			// setup: simulation
			TransitQSimulation qsim = new TransitQSimulation(scenario, new EventsManagerImpl());
			QNetwork qnet = qsim.getQNetwork();
			this.qlink1 = (QLinkImpl) qnet.getQLink(id1);
			this.qlink2 = (QLinkImpl) qnet.getQLink(id2);
			this.qlink3 = (QLinkImpl) qnet.getQLink(id3);
			this.simEngine = qsim.getQSimEngine();
//			this.simEngine = new TestSimEngine(qsim);
			this.simEngine.onPrepareSim();
			TransitStopAgentTracker tracker = qsim.getAgentTracker();
			tracker.addAgentToStop(new FakeAgent(null, null), stop1); // just add some agent so the transit vehicle has to stop
			if (stop2 != null) {
				tracker.addAgentToStop(new FakeAgent(null, null), stop2); // just add some agent so the transit vehicle has to stop
			}
			QSimTimer.setTime(100);

			// setup: vehicles
			BasicVehicleType vehicleType = new BasicVehicleTypeImpl(new IdImpl("testVehicleType"));
			BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
			capacity.setSeats(Integer.valueOf(101));
			capacity.setStandingRoom(Integer.valueOf(0));
			vehicleType.setCapacity(capacity);

			TransitDriver tDriver = new FakeTransitDriver(tLine, tRoute, dep, tracker, qsim);
			this.transitVehicle = new TransitQVehicle(new BasicVehicleImpl(tDriver.getPerson().getId(), vehicleType), 1.0);
			this.qlink1.addParkedVehicle(this.transitVehicle);
			this.transitVehicle.setEarliestLinkExitTime(100);
			this.transitVehicle.setDriver(tDriver);
			this.transitVehicle.setStopHandler(new SimpleTransitStopHandler());
			tDriver.setVehicle(this.transitVehicle);
			tDriver.activityEnds(100);

			this.normalVehicle = new QVehicleImpl(new BasicVehicleImpl(id2, vehicleType));
			this.qlink1.addParkedVehicle(this.normalVehicle);

			QPersonAgent nDriver = new QPersonAgent(person, qsim);
			this.normalVehicle.setDriver(nDriver);
			nDriver.setVehicle(this.normalVehicle);
			nDriver.initialize();
			nDriver.activityEnds(100);

			if (stop2 != null) {
				/* we're testing two stops. Add another normal vehicle with 20 seconds delay,
				 * that *could* overtake a transit vehicle at its second stop. */
				this.normalVehicle2 = new QVehicleImpl(new BasicVehicleImpl(id3, vehicleType));
				this.qlink1.addParkedVehicle(this.normalVehicle2);

				Person person2 = pb.createPerson(id3);
				Plan plan2 = pb.createPlan();
				person2.addPlan(plan2);
				Activity act2 = pb.createActivityFromLinkId("home", id1);
				act2.setEndTime(120);
				plan2.addActivity(act2);
				Leg leg2 = pb.createLeg(TransportMode.car);
				LinkNetworkRouteImpl route2 = new LinkNetworkRouteImpl(links[1].getId(), links[3].getId());
				route2.setLinkIds(links[1].getId(), linkIds_2, links[3].getId());
				leg2.setRoute(route2);
				plan2.addLeg(leg2);
				plan2.addActivity(pb.createActivityFromLinkId("work", id2));
				population.addPerson(person2);

				QPersonAgent nDriver2 = new QPersonAgent(person2, qsim);
				this.normalVehicle2.setDriver(nDriver2);
				nDriver2.setVehicle(this.normalVehicle);
				nDriver2.initialize();
			} else {
				this.normalVehicle2 = null;
			}
		}
	}

	/**
	 * A simple extension of {@link TransitDriver} that ignores the fact
	 * when there are still people in the vehicle after the last stop is
	 * handled.
	 *
	 * @author mrieser
	 */
	private static class FakeTransitDriver extends TransitDriver {

		public FakeTransitDriver(final TransitLine line, final TransitRoute route, final Departure departure,
				final TransitStopAgentTracker agentTracker, final TransitQSimulation sim) {
			super(line, route, departure, agentTracker, sim);
		}

		@Override
		public double handleTransitStop(final TransitStopFacility stop, final double now) {
			try {
				return super.handleTransitStop(stop, now);
			} catch (RuntimeException e) {
				/* the Exception is most likely thrown when there are still agents after the last stop
				 * we don't care about that, so just return 0.0. */
				return 0.0;
			}
		}

	}

}
