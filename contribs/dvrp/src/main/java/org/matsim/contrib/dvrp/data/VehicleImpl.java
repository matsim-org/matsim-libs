/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleImpl;
import org.matsim.contrib.util.LinkProvider;

/**
 * @author michalm
 */
public class VehicleImpl implements Vehicle {
	public static VehicleImpl createFromSpecification(DvrpVehicleSpecification specification,
			LinkProvider<Id<Link>> linkProvider) {
		return new VehicleImpl(specification.getId(), linkProvider.apply(specification.getStartLinkId()),
				specification.getCapacity(), specification.getServiceBeginTime(), specification.getServiceEndTime());
	}

	private final Id<Vehicle> id;
	private Link startLink;
	private final int capacity;

	// time window
	private final double serviceBeginTime;
	private double serviceEndTime;

	private Schedule schedule;

	public VehicleImpl(Id<Vehicle> id, Link startLink, int capacity, double serviceBeginTime, double serviceEndTime) {
		this.id = id;
		this.startLink = startLink;
		this.capacity = capacity;
		this.serviceBeginTime = serviceBeginTime;
		this.serviceEndTime = serviceEndTime;

		schedule = new ScheduleImpl(this);
	}

	@Override
	public Id<Vehicle> getId() {
		return id;
	}

	@Override
	public Link getStartLink() {
		return startLink;
	}

	@Override
	public void setStartLink(Link link) {
		this.startLink = link;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public double getServiceBeginTime() {
		return serviceBeginTime;
	}

	@Override
	public double getServiceEndTime() {
		return serviceEndTime;
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public String toString() {
		return "Vehicle_" + id;
	}

	public void setServiceEndTime(double serviceEndTime) {
		this.serviceEndTime = serviceEndTime;
	}

	@Override
	public void resetSchedule() {
		schedule = new ScheduleImpl(this);
	}
}
