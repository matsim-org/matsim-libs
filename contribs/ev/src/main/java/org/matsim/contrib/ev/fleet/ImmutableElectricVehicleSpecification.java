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

package org.matsim.contrib.ev.fleet;

import java.util.Objects;

import org.matsim.api.core.v01.Id;

import com.google.common.collect.ImmutableList;

/**
 * Immutable implementation of ElectricVehicleSpecification
 *
 * @author Michal Maciejewski (michalm)
 */
public final class ImmutableElectricVehicleSpecification implements ElectricVehicleSpecification {
	private final Id<ElectricVehicle> id;
	private final String vehicleType;
	private final ImmutableList<String> chargerTypes;
	private final double initialSoc;
	private final double batteryCapacity;

	private ImmutableElectricVehicleSpecification(Builder builder) {
		id = Objects.requireNonNull(builder.id);
		vehicleType = Objects.requireNonNull(builder.vehicleType);
		chargerTypes = Objects.requireNonNull(builder.chargerTypes);
		initialSoc = Objects.requireNonNull(builder.initialSoc);
		batteryCapacity = Objects.requireNonNull(builder.batteryCapacity);

		if (initialSoc < 0 || initialSoc > batteryCapacity) {
			throw new IllegalArgumentException("Invalid initialSoc/batteryCapacity of vehicle: " + id);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(ElectricVehicleSpecification copy) {
		Builder builder = new Builder();
		builder.id = copy.getId();
		builder.vehicleType = copy.getVehicleType();
		builder.chargerTypes = copy.getChargerTypes();
		builder.initialSoc = copy.getInitialSoc();
		builder.batteryCapacity = copy.getBatteryCapacity();
		return builder;
	}

	@Override
	public Id<ElectricVehicle> getId() {
		return id;
	}

	@Override
	public String getVehicleType() {
		return vehicleType;
	}

	@Override
	public ImmutableList<String> getChargerTypes() {
		return chargerTypes;
	}

	@Override
	public double getInitialSoc() {
		return initialSoc;
	}

	@Override
	public double getBatteryCapacity() {
		return batteryCapacity;
	}

	public static final class Builder {
		private Id<ElectricVehicle> id;
		private String vehicleType;
		private ImmutableList<String> chargerTypes;
		private Double initialSoc;
		private Double batteryCapacity;

		private Builder() {
		}

		public Builder id(Id<ElectricVehicle> val) {
			id = val;
			return this;
		}

		public Builder vehicleType(String val) {
			vehicleType = val;
			return this;
		}

		public Builder chargerTypes(ImmutableList<String> val) {
			chargerTypes = val;
			return this;
		}

		public Builder initialSoc(double val) {
			initialSoc = val;
			return this;
		}

		public Builder batteryCapacity(double val) {
			batteryCapacity = val;
			return this;
		}

		public ImmutableElectricVehicleSpecification build() {
			return new ImmutableElectricVehicleSpecification(this);
		}
	}
}
