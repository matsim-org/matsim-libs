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

package org.matsim.ptproject.qsim.qnetsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.PlanDriverAgent;
import org.matsim.vehicles.Vehicle;

public class QVehicleImpl implements QVehicle {

	private double linkEnterTime = Double.NaN;
	private double earliestLinkExitTime = 0;

	private PlanDriverAgent driver = null;

	private final Id id;
	
	private Link currentLink = null;
	
	private final double sizeInEquivalents;
	
	private final Vehicle basicVehicle;

	public QVehicleImpl(final Vehicle basicVehicle) {
		this(basicVehicle, 1.0);
	}
	
	public QVehicleImpl(final Vehicle basicVehicle, final double sizeInEquivalents) {
		this.id = basicVehicle.getId();
		this.sizeInEquivalents = sizeInEquivalents;
		this.basicVehicle = basicVehicle;
	}

	@Override
	public double getLinkEnterTime() {
		return this.linkEnterTime;
	}
	
	@Override
	public void setLinkEnterTime(final double time) {
		this.linkEnterTime = time;
	}
	
	@Override
	public double getEarliestLinkExitTime() {
		return this.earliestLinkExitTime;
	}

	@Override
	public void setEarliestLinkExitTime(final double time) {
		this.earliestLinkExitTime = time;
	}

	@Override
	public Link getCurrentLink() {
		return this.currentLink;
	}
	
	@Override
	public void setCurrentLink(final Link link) {
		this.currentLink = link;
	}

	@Override
	public PlanDriverAgent getDriver() {
		return this.driver;
	}

	@Override
	public void setDriver(final PlanDriverAgent driver) {
		this.driver = driver;
	}

	@Override
	public Id getId() {
		return this.id;
	}
	
	@Override
	public double getSizeInEquivalents() {
		return this.sizeInEquivalents;
	}

	@Override
	public Vehicle getVehicle() {
		return this.basicVehicle;
	}
	
	@Override
	public String toString() {
		return "Vehicle Id " + getId() + ", driven by (personId) " + this.driver.getId()
				+ ", on link " + this.currentLink.getId();
	}
	
}
