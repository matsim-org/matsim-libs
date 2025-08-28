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

package org.matsim.contrib.drt.passenger;

import com.google.common.base.MoreObjects;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author michalm
 */
public class DrtRequest implements PassengerRequest {
	private final Id<Request> id;
	private final double submissionTime;

	private final List<Id<Person>> passengerIds = new ArrayList<>();
	private final String mode;

	private final Link fromLink;
	private final Link toLink;
	private final DvrpLoad load;

	private final DrtRouteConstraints constraints;

	private DrtRequest(Builder builder) {
		id = builder.id;
		submissionTime = builder.submissionTime;
		constraints = builder.constraints;
		passengerIds.addAll(builder.passengerIds);
		mode = builder.mode;
		fromLink = builder.fromLink;
		toLink = builder.toLink;
		this.load = builder.load;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(DrtRequest copy) {
		Builder builder = new Builder();
		builder.id = copy.getId();
		builder.submissionTime = copy.getSubmissionTime();
		builder.passengerIds = new ArrayList<>(copy.getPassengerIds());
		builder.mode = copy.getMode();
		builder.fromLink = copy.getFromLink();
		builder.toLink = copy.getToLink();
		builder.constraints = copy.constraints;
		builder.load = copy.load;
		return builder;
	}

	@Override
	public Id<Request> getId() {
		return id;
	}

	@Override
	public double getSubmissionTime() {
		return submissionTime;
	}

	@Override
	public double getEarliestStartTime() {
		return constraints.earliestStartTime();
	}

	@Override
	public double getLatestStartTime() {
		return constraints.latestStartTime();
	}

	public DrtRouteConstraints getConstraints() {
		return constraints;
	}

	@Override
	public Link getFromLink() {
		return fromLink;
	}

	@Override
	public Link getToLink() {
		return toLink;
	}

	@Override
	public List<Id<Person>> getPassengerIds() {
		return List.copyOf(passengerIds);
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public DvrpLoad getLoad() {
		return this.load;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("submissionTime", submissionTime)
				.add("earliestStartTime", constraints.earliestStartTime())
				.add("latestStartTime", constraints.latestStartTime())
				.add("latestArrivalTime", constraints.latestArrivalTime())
				.add("maxRideDuration", constraints.maxRideDuration())
				.add("passengerIds", passengerIds.stream().map(Object::toString).collect(Collectors.joining(",")))
				.add("mode", mode)
				.add("fromLink", fromLink)
				.add("toLink", toLink)
				.toString();
	}

	public static final class Builder {
		private Id<Request> id;
		private double submissionTime;

		private DrtRouteConstraints constraints = DrtRouteConstraints.UNDEFINED;
		private List<Id<Person>> passengerIds = new ArrayList<>();

		private String mode;
		private Link fromLink;
		private Link toLink;
		private DvrpLoad load;

		private Builder() {
		}

		public Builder id(Id<Request> val) {
			id = val;
			return this;
		}

		public Builder submissionTime(double val) {
			submissionTime = val;
			return this;
		}

		public Builder constraints(DrtRouteConstraints val) {
			constraints = val;
			return this;
		}

		public Builder passengerIds(List<Id<Person>> val) {
			passengerIds = new ArrayList<>(val);
			return this;
		}

		public Builder mode(String val) {
			mode = val;
			return this;
		}

		public Builder fromLink(Link val) {
			fromLink = val;
			return this;
		}

		public Builder toLink(Link val) {
			toLink = val;
			return this;
		}

		public Builder load(DvrpLoad load) {
			this.load = load;
			return this;
		}

		public DrtRequest build() {
			return new DrtRequest(this);
		}
	}
}
