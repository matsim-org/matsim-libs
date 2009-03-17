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
import org.matsim.interfaces.basic.v01.population.BasicLeg;
import org.matsim.interfaces.basic.v01.population.BasicPlanElement;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.mobsim.queuesim.DriverAgent;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueVehicle;
import org.matsim.mobsim.queuesim.Simulation;
import org.matsim.population.LegImpl;
import org.matsim.population.PersonImpl;

import playground.marcel.pt.implementations.TransitDriver;

public class QueueTransitDriver implements DriverAgent {

	private final Person dummyPerson = new PersonImpl(new IdImpl("busDrvr"));
	private final Leg currentLeg = new LegImpl(BasicLeg.Mode.car);
	private final TransitDriver driver;
	private Link currentLink;
	
	public QueueTransitDriver(TransitDriver driver) {
		this.driver = driver;
		currentLeg.setRoute(driver.getCarRoute());
		this.currentLink = driver.chooseNextLink();
		driver.enterNextLink();
	}

	public Link chooseNextLink() {
		return this.driver.chooseNextLink();
	}

	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}
	
	public Person getPerson() {
		return this.dummyPerson;
	}
	
	public void incCurrentNode() {
		this.currentLink = this.driver.chooseNextLink();
		this.driver.enterNextLink();
	}

	public double getDepartureTime() {
		return this.driver.getDepartureTime();
	}
	
	public void reachActivity(double now, QueueLink currentQueueLink) {
		Simulation.decLiving();
	}
	
	// *** The methods below are currently not used... ***
	
	public List<? extends BasicPlanElement> getActsLegs() {
		return null;
	}
	
	public int getCurrentNodeIndex() {
		return 0;
	}

	public int getNextActivity() {
		return 0;
	}

	public void leaveActivity(double now) {
	}

	public void setCurrentLink(Link link) {
	}

	public void setVehicle(QueueVehicle veh) {
	}
	
}
