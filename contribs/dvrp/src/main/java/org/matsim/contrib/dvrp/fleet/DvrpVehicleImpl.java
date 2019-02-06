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
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.ScheduleImpl;
import org.matsim.contrib.util.LinkProvider;

import com.google.common.base.MoreObjects;

/**
 * @author michalm
 */
public class DvrpVehicleImpl implements DvrpVehicle {

	public static DvrpVehicleImpl createWithLinkProvider(DvrpVehicleSpecification specification,
			LinkProvider<Id<Link>> linkProvider) {
		return new DvrpVehicleImpl(specification, linkProvider.apply(specification.getStartLinkId()));
	}

	private final DvrpVehicleSpecification specification;
	private Link startLink; //FIXME will be final after removing setStartLink
	private Schedule schedule;

	public DvrpVehicleImpl(DvrpVehicleSpecification specification, Link startLink) {
		this.specification = specification;
		this.startLink = startLink;

		schedule = new ScheduleImpl(specification);
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return specification.getId();
	}

	@Override
	public Link getStartLink() {
		return startLink;
	}

	//FIXME will be removed after limiting the DvrpVehicle lifespan to single QSim simulation
	@Override
	public void setStartLink(Link link) {
		this.startLink = link;
	}

	@Override
	public int getCapacity() {
		return specification.getCapacity();
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

	@Override
	public void resetSchedule() {
		schedule = new ScheduleImpl(specification);
	}
}
