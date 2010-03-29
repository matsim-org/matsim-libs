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

package soc.ai.matsim.dbsim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.BasicVehicle;

public class DBSimVehicleImpl implements DBSimVehicle {

	private double linkEnterTime = Double.NaN;
	private double earliestLinkExitTime = 0;

	private DriverAgent driver = null;

	private final Id id;
	
	private Link currentLink = null;
	
	private final double sizeInEquivalents;
	
	private final BasicVehicle basicVehicle;

	public DBSimVehicleImpl(final BasicVehicle basicVehicle) {
		this(basicVehicle, 1.0);
	}
	
	public DBSimVehicleImpl(final BasicVehicle basicVehicle, final double sizeInEquivalents) {
		this.id = basicVehicle.getId();
		this.sizeInEquivalents = sizeInEquivalents;
		this.basicVehicle = basicVehicle;
	}

	public double getLinkEnterTime() {
		return this.linkEnterTime;
	}
	
	public void setLinkEnterTime(final double time) {
		this.linkEnterTime = time;
	}
	
	public double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	public void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	public Link getCurrentLink() {
		return this.currentLink;
	}
	
	public void setCurrentLink(final Link link) {
		this.currentLink = link;
	}

	public DriverAgent getDriver() {
		return this.driver;
	}

	public void setDriver(final DriverAgent driver) {
		this.driver = driver;
	}

	public Id getId() {
		return this.id;
	}
	
	public double getSizeInEquivalents() {
		return this.sizeInEquivalents;
	}

	public BasicVehicle getBasicVehicle() {
		return this.basicVehicle;
	}
	
	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getPerson().getId()
				+ ", on link " + this.currentLink.getId();
	}
	
}
