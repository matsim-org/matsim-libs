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

import java.util.List;

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
	private final double[] slackTimes;// for all insertion points (start, stops, end)
	private final List<Double> precedingStayTimes;// for all stops
	public final double createTime;

	public VehicleEntry(DvrpVehicle vehicle, Waypoint.Start start, ImmutableList<Waypoint.Stop> stops,
			double[] slackTimes, List<Double> precedingStayTimes, double createTime) {
		this.vehicle = vehicle;
		this.start = start;
		this.stops = stops;
		this.end = Waypoint.End.OPEN_END;
		this.slackTimes = slackTimes;
		this.precedingStayTimes = precedingStayTimes;
		this.createTime = createTime;
	}

	protected VehicleEntry(VehicleEntry that) {
		this.vehicle = that.vehicle;
		this.start = that.start;
		this.stops = that.stops;
		this.end = that.end;
		this.slackTimes = that.slackTimes;
		this.precedingStayTimes = that.precedingStayTimes;
		this.createTime = that.createTime;
	}

	public Waypoint getWaypoint(int index) {
		return index == 0 ? start : (index == stops.size() + 1 ? end : stops.get(index - 1));
	}

	public boolean isAfterLastStop(int index) {
		return index == stops.size();
	}

	public double getSlackTime(int index) {
		return slackTimes[index + 1];
	}
	
	public double getStartSlackTime() {
		return slackTimes[0];
	}
	
	public double getPrecedingStayTime(int index) {
		return precedingStayTimes.get(index);
	}
}
