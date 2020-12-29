/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import java.util.Optional;
import java.util.stream.DoubleStream;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface Waypoint {
	Link getLink();

	double getArrivalTime();

	double getDepartureTime();

	int getOutgoingOccupancy();

	class Start implements Waypoint {
		public final Optional<Task> task;// empty if schedule status is PLANNED
		public final Link link;
		public final double time;
		public final int occupancy;

		public Start(@Nullable Task task, Link link, double time, int occupancy) {
			this.task = Optional.ofNullable(task);
			this.link = link;
			this.time = time;
			this.occupancy = occupancy;
		}

		@Override
		public Link getLink() {
			return link;
		}

		@Override
		public double getArrivalTime() {
			throw new UnsupportedOperationException("No arrival time for start waypoint");
		}

		@Override
		public double getDepartureTime() {
			return time;
		}

		@Override
		public int getOutgoingOccupancy() {
			return occupancy;
		}
	}

	class End implements Waypoint {
		public static final End OPEN_END = new End();

		@Nullable
		public final Link link;//null if open-end route
		public final OptionalTime arrivalTime;//undefined if open-end route

		private End() {
			link = null;
			arrivalTime = OptionalTime.undefined();
		}

		public End(Link link, double arrivalTime) {
			this.link = link;
			this.arrivalTime = OptionalTime.defined(arrivalTime);
		}

		public boolean isOpenEnd() {
			return link == null;
		}

		@Override
		@Nullable
		public Link getLink() {
			return link;
		}

		@Override
		public double getArrivalTime() {
			return arrivalTime.seconds();
		}

		@Override
		public double getDepartureTime() {
			throw new UnsupportedOperationException("No departure time for end waypoint");
		}

		@Override
		public int getOutgoingOccupancy() {
			throw new UnsupportedOperationException("End is the terminal waypoint");
		}
	}

	class Stop implements Waypoint {
		public final DrtStopTask task;
		public final double latestArrivalTime;// relating to max passenger drive time (for dropoff requests)
		public final double latestDepartureTime;// relating to passenger max wait time (for pickup requests)
		public final int occupancyChange;// diff in pickups and dropoffs
		public final int outgoingOccupancy;

		public Stop(DrtStopTask task, int outputOccupancy) {
			this.task = task;
			this.outgoingOccupancy = outputOccupancy;

			latestArrivalTime = calcLatestArrivalTime();
			// essentially the min of the latest possible arrival times at this stop

			latestDepartureTime = calcLatestDepartureTime();
			// essentially the min of the latest possible pickup times at this stop

			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		@Override
		public Link getLink() {
			return task.getLink();
		}

		@Override
		public double getArrivalTime() {
			return task.getBeginTime();
		}

		@Override
		public double getDepartureTime() {
			return task.getEndTime();
		}

		@Override
		public int getOutgoingOccupancy() {
			return outgoingOccupancy;
		}

		private double calcLatestArrivalTime() {
			return getMaxTimeConstraint(
					task.getDropoffRequests().values().stream().mapToDouble(DrtRequest::getLatestArrivalTime),
					task.getBeginTime());
		}

		private double calcLatestDepartureTime() {
			return getMaxTimeConstraint(
					task.getPickupRequests().values().stream().mapToDouble(DrtRequest::getLatestStartTime),
					task.getEndTime());
		}

		private double getMaxTimeConstraint(DoubleStream latestAllowedTimes, double scheduledTime) {
			//XXX if task is already delayed beyond one or more of latestTimes, use scheduledTime as maxTime constraint
			//thus we can still add a new request to the already scheduled stops (as no further delays are incurred)
			//but we cannot add a new stop before the delayed task
			return Math.max(latestAllowedTimes.min().orElse(Double.MAX_VALUE), scheduledTime);
		}

		@Override
		public String toString() {
			return "VehicleData.Stop for: " + task.toString();
		}
	}
}
