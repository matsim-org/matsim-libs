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

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.DoubleStream;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.MoreObjects;

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

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("task", task)
					.add("link", link)
					.add("time", time)
					.add("occupancy", occupancy)
					.toString();
		}
	}

	class End implements Waypoint {
		public static final End OPEN_END = new End();

		public final Optional<Link> link;//null if open-end route
		public final OptionalTime arrivalTime;//undefined if open-end route

		private End() {
			link = Optional.empty();
			arrivalTime = OptionalTime.undefined();
		}

		public End(Link link, double arrivalTime) {
			this.link = Optional.of(link);
			this.arrivalTime = OptionalTime.defined(arrivalTime);
		}

		public boolean isOpenEnd() {
			return link.isEmpty();
		}

		@Override
		public Link getLink() {
			return link.orElseThrow(() -> new NoSuchElementException("Open-end route -- no end link provided"));
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

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("link", link).add("arrivalTime", arrivalTime).toString();
		}
	}

	class Stop implements Waypoint {
		public final DrtStopTask task;
		public final double latestArrivalTime;// relating to max passenger drive time (for dropoff requests)
		public final double latestDepartureTime;// relating to passenger max wait time (for pickup requests)
		public final int outgoingOccupancy;

		public Stop(DrtStopTask task, int outgoingOccupancy) {
			this.task = task;
			this.outgoingOccupancy = outgoingOccupancy;

			// essentially the min of the latest possible arrival times at this stop
			latestArrivalTime = calcLatestArrivalTime();

			// essentially the min of the latest possible pickup times at this stop
			latestDepartureTime = calcLatestDepartureTime();
		}

		public Stop(DrtStopTask task, double latestArrivalTime, double latestDepartureTime, int outgoingOccupancy) {
			this.task = task;
			this.latestArrivalTime = latestArrivalTime;
			this.latestDepartureTime = latestDepartureTime;
			this.outgoingOccupancy = outgoingOccupancy;
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

		public int getOccupancyChange() {
			return task.getPickupRequests().values().stream().mapToInt(AcceptedDrtRequest::getPassengerCount).sum() -
					task.getDropoffRequests().values().stream().mapToInt(AcceptedDrtRequest::getPassengerCount).sum();
		}

		private double calcLatestArrivalTime() {
			return getMaxTimeConstraint(
					task.getDropoffRequests().values().stream().mapToDouble(AcceptedDrtRequest::getLatestArrivalTime),
					task.getBeginTime());
		}

		private double calcLatestDepartureTime() {
			return getMaxTimeConstraint(
					task.getPickupRequests().values().stream().mapToDouble(AcceptedDrtRequest::getLatestStartTime),
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

	class Pickup implements Waypoint {
		public final DrtRequest request;

		public Pickup(DrtRequest request) {
			this.request = request;
		}

		@Override
		public Link getLink() {
			return request.getFromLink();
		}

		@Override
		public double getArrivalTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getDepartureTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getOutgoingOccupancy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("request", request).toString();
		}
	}

	class Dropoff implements Waypoint {
		public final DrtRequest request;

		public Dropoff(DrtRequest request) {
			this.request = request;
		}

		@Override
		public Link getLink() {
			return request.getToLink();
		}

		@Override
		public double getArrivalTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getDepartureTime() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getOutgoingOccupancy() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("request", request).toString();
		}
	}
}
