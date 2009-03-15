/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueDriver.java
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

package playground.marcel.pt.integration;

import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.core.v01.CarRoute;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.mobsim.queuesim.DriverAgent;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.mobsim.queuesim.Simulation;
import org.matsim.population.LegImpl;
import org.matsim.population.PersonImpl;

import playground.marcel.pt.tryout.BusDriver;

public class TransitQueueDriver implements DriverAgent {

	private final Person dummyPerson = new PersonImpl(new IdImpl("busDrvr"));
	private final Leg currentLeg = new LegImpl(BasicLeg.Mode.car);
	private final BusDriver driver;
	private Link currentLink;
	
	public TransitQueueDriver(BusDriver driver) {
		this.driver = driver;
		final CarRoute currentRoute = driver.getCarRoute();// new NodeCarRoute();
//		currentRoute.setNodes(null, new ArrayList<Node>(), null);
		currentLeg.setRoute(currentRoute);
		
		this.currentLink = driver.chooseNextLink();
		driver.enterNextLink();
	}

	public Link chooseNextLink() {
		return this.driver.chooseNextLink();
	}

	public List<Object> getActsLegs() {
		// TODO [MR] Auto-generated method stub
		return null;
	}

	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

	public int getCurrentNodeIndex() {
		// TODO [MR] Auto-generated method stub
		return 0;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}

	public int getNextActivity() {
		// TODO [MR] Auto-generated method stub
		return 0;
	}

	public Person getPerson() {
		return this.dummyPerson;
	}

	public void incCurrentNode() {
		this.currentLink = this.driver.chooseNextLink();
		this.driver.enterNextLink();
	}

	public void leaveActivity(double now) {
		// TODO [MR] Auto-generated method stub
		
	}

	public void reachActivity(double now, QueueLink currentQueueLink) {
		Simulation.decLiving();
	}

	public void setCurrentLink(Link link) {
		// TODO [MR] Auto-generated method stub
		
	}

	public void setVehicle(QueueVehicle veh) {
		// TODO [MR] Auto-generated method stub
		
	}

	public double getDepartureTime() {
		return this.driver.getDepartureTime();
	}
	
	
}
