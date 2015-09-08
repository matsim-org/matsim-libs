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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;


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
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, Id.create(1, Person.class), Id.create(1, Link.class), null);
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

		LinkEnterEvent enterEvent2 = new LinkEnterEvent(10.0, Id.create(2, Person.class), Id.create(1, Link.class), null);
		sensor.handleEvent(enterEvent2);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(2, numberOfCars);
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, Id.create(1, Person.class), Id.create(1, Link.class), null);
		sensor.handleEvent(leaveEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);
		
		PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(110.0, Id.create(2, Person.class), Id.create(1, Link.class), "car");
		sensor.handleEvent(arrivalEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(0, numberOfCars);
		
		Wait2LinkEvent wait2LinkEvent = new Wait2LinkEvent(120.0, Id.create(2, Person.class), Id.create(1, Link.class), null, 1.0);
		sensor.handleEvent(wait2LinkEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		leaveEvent = new LinkLeaveEvent(120.0, Id.create(2, Person.class), Id.create(1, Link.class), null);
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
		
		LinkEnterEvent enterEvent = new LinkEnterEvent(0.0, Id.create(1, Person.class), Id.create(1, Link.class), null);
		sensor.handleEvent(enterEvent);
		numberOfCars = sensor.getNumberOfCarsOnLink();
		Assert.assertEquals(1, numberOfCars);

		enterEvent = new LinkEnterEvent(1.0, Id.create(2, Person.class), Id.create(1, Link.class), null);
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
		
		LinkLeaveEvent leaveEvent = new LinkLeaveEvent(100.0, Id.create(1, Person.class), Id.create(1, Link.class), null);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 100.0);
		Assert.assertEquals(1, numberOfCarsInDistance);
		
		PersonArrivalEvent arrivalEvent = new PersonArrivalEvent(101.0, Id.create(2, Person.class), Id.create(1, Link.class), "car");
		sensor.handleEvent(arrivalEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 101.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
		Wait2LinkEvent wait2LinkEvent = new Wait2LinkEvent(120.0, Id.create(2, Person.class), Id.create(1, Link.class), null, 1.0);
		sensor.handleEvent(wait2LinkEvent);

		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(1, numberOfCarsInDistance);

		leaveEvent = new LinkLeaveEvent(120.0, Id.create(2, Person.class), Id.create(1, Link.class), null);
		sensor.handleEvent(leaveEvent);
		
		numberOfCarsInDistance = sensor.getNumberOfCarsInDistance(100.0, 120.0);
		Assert.assertEquals(0, numberOfCarsInDistance);
		
	}

}
