/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.passenger;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.schedule.RequestTiming;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.List;

/**
 * @author Michal Maciejewski (michalm)
 */
public class AcceptedDrtRequest {
	public static AcceptedDrtRequest createFromOriginalRequest(DrtRequest request, double dropoffDuration) {
		return AcceptedDrtRequest.newBuilder()
				.request(request)
				.earliestStartTime(request.getEarliestStartTime())
				.latestStartTime(request.getLatestStartTime())
				.latestArrivalTime(request.getLatestArrivalTime())
				.maxRideDuration(request.getMaxRideDuration())
				.dropoffDuration(dropoffDuration)
				.build();
	}

	public static AcceptedDrtRequest createFromOriginalRequest(DrtRequest request) {
		return createFromOriginalRequest(request, 0.0);
	}

	private final DrtRequest request;

	private final double earliestStartTime;
	private final double latestStartTime;
	private final double latestArrivalTime;
	private final double maxRideDuration;
	private final double dropoffDuration;
	private final RequestTiming requestTiming;

	private AcceptedDrtRequest(Builder builder) {
		request = builder.request;
		earliestStartTime = builder.earliestStartTime;
		latestStartTime = builder.latestStartTime;
		latestArrivalTime = builder.latestArrivalTime;
		maxRideDuration = builder.maxRideDuration;
		dropoffDuration = builder.dropoffDuration;
		requestTiming = new RequestTiming(builder.plannedPickupTime, builder.plannedDropoffTime);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(AcceptedDrtRequest copy) {
		Builder builder = new Builder();
		builder.request = copy.getRequest();
		builder.earliestStartTime = copy.getEarliestStartTime();
		builder.latestStartTime = copy.getLatestStartTime();
		builder.latestArrivalTime = copy.getLatestArrivalTime();
		builder.maxRideDuration = copy.getMaxRideDuration();
		builder.dropoffDuration = copy.getDropoffDuration();
		copy.requestTiming.getPlannedPickupTime().ifDefined(val -> builder.plannedPickupTime = val);
		copy.requestTiming.getPlannedDropoffTime().ifDefined(val -> builder.plannedDropoffTime = val);
		return builder;
	}

	public DrtRequest getRequest() {
		return request;
	}

	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	public double getLatestStartTime() {
		return latestStartTime;
	}

	public double getLatestArrivalTime() {
		return latestArrivalTime;
	}
	public double getMaxRideDuration() {
		return maxRideDuration;
	}

	public double getDropoffDuration() {
		return dropoffDuration;
	}

	public Id<Request> getId() {
		return request.getId();
	}

	public double getSubmissionTime() {
		return request.getSubmissionTime();
	}

	public Link getFromLink() {
		return request.getFromLink();
	}

	public Link getToLink() {
		return request.getToLink();
	}

	public List<Id<Person>> getPassengerIds() {
		return request.getPassengerIds();
	}

	public DvrpLoad getLoad() {
		return request.getLoad();
	}

	public String getMode() {
		return request.getMode();
	}

	public RequestTiming getRequestTiming() {
		return requestTiming;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("request", request)
				.add("earliestStartTime", earliestStartTime)
				.add("latestStartTime", latestStartTime)
				.add("latestArrivalTime", latestArrivalTime)
				.toString();
	}

	public static final class Builder {
		private DrtRequest request;
		private double earliestStartTime;
		private double latestStartTime;
		private double latestArrivalTime;
		private double maxRideDuration;
		private double dropoffDuration;
		private double plannedPickupTime = RequestTiming.UNDEFINED_TIME;
		private double plannedDropoffTime = RequestTiming.UNDEFINED_TIME;

		private Builder() {
		}

		public Builder request(DrtRequest val) {
			request = val;
			return this;
		}

		public Builder earliestStartTime(double val) {
			earliestStartTime = val;
			return this;
		}

		public Builder latestStartTime(double val) {
			latestStartTime = val;
			return this;
		}

		public Builder latestArrivalTime(double val) {
			latestArrivalTime = val;
			return this;
		}

		public Builder maxRideDuration(double val) {
			this.maxRideDuration = val;
			return this;
		}

		public Builder dropoffDuration(double val) {
			this.dropoffDuration = val;
			return this;
		}

		public Builder plannedPickupTime(double val) {
			plannedPickupTime = val;
			return this;
		}

		public Builder plannedDropoffTime(double val) {
			plannedDropoffTime = val;
			return this;
		}

		public AcceptedDrtRequest build() {
			return new AcceptedDrtRequest(this);
		}
	}
}
