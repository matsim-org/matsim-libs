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

package org.matsim.contrib.dvrp.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Immutable implementation of DvrpVehicleSpecification
 *
 * @author Michal Maciejewski (michalm)
 */
public final class ImmutableDvrpVehicleSpecification implements DvrpVehicleSpecification {
	private final Id<Vehicle> id;
	private final Id<Link> startLinkId;
	private final int capacity;

	// time window
	private final double serviceBeginTime;
	private final double serviceEndTime;

	private ImmutableDvrpVehicleSpecification(Builder builder) {
		id = builder.id;
		startLinkId = builder.startLinkId;
		capacity = builder.capacity;
		serviceBeginTime = builder.serviceBeginTime;
		serviceEndTime = builder.serviceEndTime;
	}

	@Override
	public Id<Vehicle> getId() {
		return id;
	}

	@Override
	public Id<Link> getStartLinkId() {
		return startLinkId;
	}

	@Override
	public int getCapacity() {
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

	public static final class Builder {
		private Id<Vehicle> id;
		private Id<Link> startLinkId;
		private int capacity;
		private double serviceBeginTime;
		private double serviceEndTime;

		public Builder() {
		}

		public Builder(DvrpVehicleSpecification copy) {
			this.id = copy.getId();
			this.startLinkId = copy.getStartLinkId();
			this.capacity = copy.getCapacity();
			this.serviceBeginTime = copy.getServiceBeginTime();
			this.serviceEndTime = copy.getServiceEndTime();
		}

		public Builder id(Id<Vehicle> val) {
			id = val;
			return this;
		}

		public Builder startLinkId(Id<Link> val) {
			startLinkId = val;
			return this;
		}

		public Builder capacity(int val) {
			capacity = val;
			return this;
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
