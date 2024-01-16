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
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.List;

/**
 * @author Michal Maciejewski (michalm)
 */
public class AcceptedDrtRequest {
	public static AcceptedDrtRequest createFromOriginalRequest(DrtRequest request) {
		return AcceptedDrtRequest.newBuilder()
				.request(request)
				.earliestStartTime(request.getEarliestStartTime())
				.latestStartTime(request.getLatestStartTime())
				.latestArrivalTime(request.getLatestArrivalTime())
				.build();
	}

	private final DrtRequest request;

	private final double earliestStartTime;
	private final double latestStartTime;
	private final double latestArrivalTime;

	private AcceptedDrtRequest(Builder builder) {
		request = builder.request;
		earliestStartTime = builder.earliestStartTime;
		latestStartTime = builder.latestStartTime;
		latestArrivalTime = builder.latestArrivalTime;
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

	public int getPassengerCount() {
		return request.getPassengerCount();
	}

	public String getMode() {
		return request.getMode();
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

		public AcceptedDrtRequest build() {
			return new AcceptedDrtRequest(this);
		}
	}
}
