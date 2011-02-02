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

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;


/**
 * @author dgrether
 *
 */
public class DgSensorTest {

	private Id id1, id2;
	
	/**
	 * Creates a Network with one 600 m link fs 6 -> fs_tt 100 s and flow_cap = 1 veh/s
	 * 1 ---------- 2
	 */
	private Scenario createScenario(){
		Scenario sc = new ScenarioImpl();
		this.id1 = sc.createId("1");
		this.id2 = sc.createId("2");
		Network net = sc.getNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n = nf.createNode(id1, sc.createCoord(0, 0));
		net.addNode(n);
		n = nf.createNode(id2, sc.createCoord(500, 0));
		net.addNode(n);
		Link l = nf.createLink(id1, id1, id2);
		net.addLink(l);
		l.setLength(600.0);
		l.setCapacity(3600);
		l.setFreespeed(6.0);
		return sc;
	}
	
	
	@Test
	public void testSensorNumberOfCarsMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(id1);
		DgSensor sensor = new DgSensor(link);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		
		LinkEnterEvent enterEvent = new LinkEnterEventImpl(0.0, id1, id1);
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

		LinkEnterEvent enterEvent2 = new LinkEnterEventImpl(10.0, id2, id1);
		sensor.handleEvent(enterEvent2);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(2, numberOfCars);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEventImpl(100.0, id1, id1);
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);
		
		AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(110.0, id2, id1, "car");
		sensor.handleEvent(arrivalEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		
		AgentWait2LinkEvent wait2LinkEvent = new AgentWait2LinkEventImpl(120.0, id2, id1);
		sensor.handleEvent(wait2LinkEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		leaveEvent = new LinkLeaveEventImpl(120.0, id2, id1);
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
	}
	
	@Test
	public void testSensorDistanceMonitoring(){
		Scenario sc = this.createScenario();
		Link link = sc.getNetwork().getLinks().get(id1);
		DgSensor sensor = new DgSensor(link);
		sensor.registerDistanceToMonitor(100.0);
		int numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		int numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 0.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
		LinkEnterEvent enterEvent = new LinkEnterEventImpl(0.0, id1, id1);
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		enterEvent = new LinkEnterEventImpl(1.0, id2, id1);
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
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEventImpl(100.0, id1, id1);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 100.0);
		Assert.assertEquals(1, numberOfCarsInDistance);
		
		AgentArrivalEvent arrivalEvent = new AgentArrivalEventImpl(101.0, id2, id1, "car");
		sensor.handleEvent(arrivalEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 101.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
		AgentWait2LinkEvent wait2LinkEvent = new AgentWait2LinkEventImpl(120.0, id2, id1);
		sensor.handleEvent(wait2LinkEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(1, numberOfCarsInDistance);

		leaveEvent = new LinkLeaveEventImpl(120.0, id2, id1);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
	}

}
