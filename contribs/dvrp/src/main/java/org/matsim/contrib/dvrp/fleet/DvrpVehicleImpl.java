/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.schedule.Schedule;

import com.google.common.base.MoreObjects;

/**
 * @author michalm
 */
public class DvrpVehicleImpl implements DvrpVehicle {
	private final DvrpVehicleSpecification specification;
	private final Link startLink;
	private final Schedule schedule;
	private DvrpLoad capacity;

	public DvrpVehicleImpl(DvrpVehicleSpecification specification, Link startLink) {
		if (startLink == null) {
			throw new RuntimeException("Start link "
					+ specification.getStartLinkId()
					+ " of vehicle "
					+ specification.getId()
					+ " is null."
					+ " Please make sure the link is part of the mode-filtered (and cleaned?) network! Aborting...");
		}
		if (!startLink.getId().equals(specification.getStartLinkId())) {
			throw new IllegalArgumentException("startLink.id != specification.startLinkId");
		}
		this.specification = specification;
		this.startLink = startLink;
		this.capacity = specification.getCapacity();
		schedule = Schedule.create(specification);
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return specification.getId();
	}

	@Override
	public Link getStartLink() {
		return startLink;
	}

	@Override
	public double getServiceBeginTime() {
		return specification.getServiceBeginTime();
	}

	@Override
	public double getServiceEndTime() {
		return specification.getServiceEndTime();
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	public DvrpVehicleSpecification getSpecification() {
		return specification;
	}

	public void setCapacity(DvrpLoad capacity) {
		this.capacity = capacity;
	}

	@Override
	public DvrpLoad getCapacity() {
		return this.capacity;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", getId())
				.add("startLinkId", getStartLink().getId())
				.add("capacity", getCapacity())
				.add("serviceBeginTime", getServiceBeginTime())
				.add("serviceEndTime", getServiceEndTime())
				.toString();
	}
}
