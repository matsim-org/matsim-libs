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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

import playground.marcel.pt.implementations.TransitDriver;

public class QueueTransitDriver implements DriverAgent {

	private final Person dummyPerson = new PersonImpl(new IdImpl("busDrvr"));
	private final Leg currentLeg = new LegImpl(TransportMode.car);
	private final TransitDriver driver;
	
	public QueueTransitDriver(TransitDriver driver) {
		this.driver = driver;
		currentLeg.setRoute(driver.getCarRoute());
		driver.enterNextLink();
	}

	public Link chooseNextLink() {
		return this.driver.chooseNextLink();
	}

	public Leg getCurrentLeg() {
		return this.currentLeg;
	}

	public Link getDestinationLink() {
		return this.currentLeg.getRoute().getEndLink();
	}
	
	public Person getPerson() {
		return this.dummyPerson;
	}
	
	public void incCurrentNode() {
		this.driver.enterNextLink();
	}

	public double getDepartureTime() {
		return this.driver.getDepartureTime();
	}
	
	// *** The methods below are currently not used... ***

	public void leaveActivity(double now) {
	}

	public void legEnds(double now) {
		// TODO [MR] Auto-generated method stub
		
	}
	
}
