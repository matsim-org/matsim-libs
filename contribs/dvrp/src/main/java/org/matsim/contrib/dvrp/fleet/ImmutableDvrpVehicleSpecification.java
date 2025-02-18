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

import java.util.Objects;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegerLoad;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.MoreObjects;

/**
 * Immutable implementation of DvrpVehicleSpecification
 *
 * @author Michal Maciejewski (michalm)
 */
public final class ImmutableDvrpVehicleSpecification implements DvrpVehicleSpecification {
	private final Id<DvrpVehicle> id;
	private final Id<Link> startLinkId;
	private final DvrpLoad capacity;

	// time window
	private final double serviceBeginTime;
	private final double serviceEndTime;

	private ImmutableDvrpVehicleSpecification(Builder builder) {
		id = Objects.requireNonNull(builder.id);
		startLinkId = Objects.requireNonNull(builder.startLinkId);
		capacity = Objects.requireNonNull(builder.capacity);
		serviceBeginTime = Objects.requireNonNull(builder.serviceBeginTime);
		serviceEndTime = Objects.requireNonNull(builder.serviceEndTime);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(DvrpVehicleSpecification copy) {
		Builder builder = new Builder();
		builder.id = copy.getId();
		builder.startLinkId = copy.getStartLinkId();
		builder.capacity = copy.getCapacity();
		builder.serviceBeginTime = copy.getServiceBeginTime();
		builder.serviceEndTime = copy.getServiceEndTime();
		return builder;
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return id;
	}

	@Override
	public Optional<Vehicle> getMatsimVehicle() {
		return Optional.empty();
	}

	@Override
	public Id<Link> getStartLinkId() {
		return startLinkId;
	}

	@Override
	public DvrpLoad getCapacity() {
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
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("startLinkId", startLinkId)
				.add("capacity", capacity)
				.add("serviceBeginTime", serviceBeginTime)
				.add("serviceEndTime", serviceEndTime)
				.toString();
	}

	public static final class Builder {
		private Id<DvrpVehicle> id;
		private Id<Link> startLinkId;
		private DvrpLoad capacity;
		private Double serviceBeginTime;
		private Double serviceEndTime;

		private Builder() {
		}

		public Builder id(Id<DvrpVehicle> val) {
			id = val;
			return this;
		}

		public Builder startLinkId(Id<Link> val) {
			startLinkId = val;
			return this;
		}

		public Builder capacity(DvrpLoad val) {
			capacity = val;
			return this;
		}

		public Builder capacity(int val) {
			return this.capacity(IntegerLoad.fromValue(val));
		}

		public Builder serviceBeginTime(double val) {
			serviceBeginTime = val;
			return this;
		}

		public Builder serviceEndTime(double val) {
			serviceEndTime = val;
			return this;
		}

		public ImmutableDvrpVehicleSpecification build() {
			return new ImmutableDvrpVehicleSpecification(this);
		}
	}
}
