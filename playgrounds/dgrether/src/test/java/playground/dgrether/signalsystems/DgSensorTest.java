/* *********************************************************************** *
 * project: org.matsim.*
 * DgSensorTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.signalsystems;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import junit.framework.Assert;


/**
 * @author dgrether
 *
 */
public class DgSensorTest {

	/**
	 * Creates a Network with one 600 m link fs 6 -> fs_tt 100 s and flow_cap = 1 veh/s
	 * 1 ---------- 2
	 */
	private Scenario createScenario(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		net.addNode(n1);
		Node n2 = nf.createNode(Id.create(2, Node.class), new Coord((double) 500, (double) 0));
		net.addNode(n2);
		Link l = nf.createLink(Id.create(1, Link.class), n1, n2);
		net.addLink(l);
		l.setLength(600.0);
		l.setCapacity(3600);
		l.setFreespeed(6.0);
		return sc;
	}
	
	
	@Test
	public void testSensorNumberOfCarsMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(Id.create(1, Link.class));
		DgSensor sensor = new DgSensor(link);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		
		Id<Person> agId1 = Id.createPersonId(1);
		Id<Person> agId2 = Id.createPersonId(2);
		Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, agId1, link.getId(), vehId1);
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);
		//expect NullPointerException as feature is still switched of
		NullPointerException e = null;
		try {
			sensor.getNumberOfCarsInDistance(600.0, 0.0);
		}
		catch (NullPointerException ex){
			e = ex;
		}
		Assert.assertNotNull(e);

		LinkEnterEvent enterEvent2 = new LinkEnterEvent(10.0, agId2, link.getId(), vehId2);
		sensor.handleEvent(enterEvent2);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(2, numberOfCars);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, agId1, link.getId(), vehId1);
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);
		
		VehicleLeavesTrafficEvent link2WaitEvent = new VehicleLeavesTrafficEvent(110.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(link2WaitEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		
		Wait2LinkEvent wait2LinkEvent = new Wait2LinkEvent(120.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(wait2LinkEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		leaveEvent = new LinkLeaveEvent(120.0, agId2, link.getId(), vehId2);
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
	}
	
	@Test
	public void testSensorDistanceMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(Id.create(1, Link.class));
		DgSensor sensor = new DgSensor(link);
		sensor.registerDistanceToMonitor(100.0);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		int numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 0.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
		Id<Person> agId1 = Id.createPersonId(1);
		Id<Person> agId2 = Id.createPersonId(2);
		Id<Vehicle> vehId1 = Id.create(1, Vehicle.class);
		Id<Vehicle> vehId2 = Id.create(2, Vehicle.class);
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, agId1, link.getId(), vehId1);
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		enterEvent = new LinkEnterEvent(1.0, agId2, link.getId(), vehId2);
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(2, numberOfCars);

		//expect NullPointerException as feature is not switched on for distance 500.0 m
		NullPointerException e = null;
		try {
			sensor.getNumberOfCarsInDistance(500.0, 0.0);
		}
		catch (NullPointerException ex){
			e = ex;
		}
		Assert.assertNotNull(e);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 10.0);
		Assert.assertEquals(0, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 83.0);
		Assert.assertEquals(0, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 84.0);
		Assert.assertEquals(1, numberOfCarsInDistance);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 85.0);
		Assert.assertEquals(2, numberOfCarsInDistance);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, agId1, link.getId(), vehId1);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 100.0);
		Assert.assertEquals(1, numberOfCarsInDistance);
		
		VehicleLeavesTrafficEvent link2WaitEvent = new VehicleLeavesTrafficEvent(101.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(link2WaitEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 101.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
		Wait2LinkEvent wait2LinkEvent = new Wait2LinkEvent(120.0, agId2, link.getId(), vehId2, TransportMode.car, 1.0);
		sensor.handleEvent(wait2LinkEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(1, numberOfCarsInDistance);

		leaveEvent = new LinkLeaveEvent(120.0, agId2, link.getId(), vehId2);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
	}

}
