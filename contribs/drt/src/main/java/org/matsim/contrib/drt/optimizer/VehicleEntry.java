/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VehicleEntry {
	public interface EntryFactory {
		VehicleEntry create(DvrpVehicle vehicle, double currentTime);
	}

	public final DvrpVehicle vehicle;
	public final Waypoint.Start start;
	public final ImmutableList<Waypoint.Stop> stops;
	public final Waypoint.End end;

	public VehicleEntry(DvrpVehicle vehicle, Waypoint.Start start, ImmutableList<Waypoint.Stop> stops) {
		this.vehicle = vehicle;
		this.start = start;
		this.stops = stops;
		this.end = Waypoint.End.OPEN_END;
	}

	public Waypoint getWaypoint(int index) {
		return index == 0 ? start : (index == stops.size() + 1 ? end : stops.get(index - 1));
	}

	public boolean isAfterLastStop(int index) {
		return index == stops.size();
	}
}
