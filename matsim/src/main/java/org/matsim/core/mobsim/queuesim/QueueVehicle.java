/* *********************************************************************** *
 * project: org.matsim.*
 * Vehicle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.matsim.core.mobsim.queuesim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.vehicles.BasicVehicle;

 class QueueVehicle {

	private double linkEnterTime = Double.NaN;
	private double earliestLinkExitTime = 0;

	private PersonDriverAgent driver = null;

	private final Id id;
	
	private Link currentLink = null;
	
	private final double sizeInEquivalents;
	
	private final BasicVehicle basicVehicle;

	/*package*/ QueueVehicle(final BasicVehicle basicVehicle) {
		this(basicVehicle, 1.0);
	}
	
	/*package*/ QueueVehicle(final BasicVehicle basicVehicle, final double sizeInEquivalents) {
		this.id = basicVehicle.getId();
		this.sizeInEquivalents = sizeInEquivalents;
		this.basicVehicle = basicVehicle;
	}

	 double getLinkEnterTime() {
		return this.linkEnterTime;
	}
	
	void setLinkEnterTime(final double time) {
		this.linkEnterTime = time;
	}
	
	 double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	 Link getCurrentLink() {
		return this.currentLink;
	}
	
	void setCurrentLink(final Link link) {
		this.currentLink = link;
	}

	 PersonDriverAgent getDriver() {
		return this.driver;
	}

	void setDriver(final PersonDriverAgent driver) {
		this.driver = driver;
	}

	 Id getId() {
		return this.id;
	}
	
	 double getSizeInEquivalents() {
		return this.sizeInEquivalents;
	}

	 BasicVehicle getBasicVehicle() {
		return this.basicVehicle;
	}
	
	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getPerson().getId()
				+ ", on link " + this.currentLink.getId();
	}
	
}
