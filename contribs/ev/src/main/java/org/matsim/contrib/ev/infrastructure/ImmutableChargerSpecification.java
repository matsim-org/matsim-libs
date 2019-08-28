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

package org.matsim.contrib.ev.infrastructure;

import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.google.common.base.MoreObjects;

/**
 * Immutable implementation of ChargerSpecification
 *
 * @author Michal Maciejewski (michalm)
 */
public class ImmutableChargerSpecification implements ChargerSpecification {
	private final Id<Charger> id;
	private final Id<Link> linkId;
	private final String chargerType;
	private final double plugPower;
	private final int plugCount;

	private ImmutableChargerSpecification(Builder builder) {
		id = Objects.requireNonNull(builder.id);
		linkId = Objects.requireNonNull(builder.linkId);
		chargerType = Objects.requireNonNull(builder.chargerType);
		plugPower = Objects.requireNonNull(builder.plugPower);
		plugCount = Objects.requireNonNull(builder.plugCount);

		if (plugPower < 0) {
			throw new IllegalArgumentException("Negative plugPower of charger: " + id);
		}
		if (plugCount < 0) {
			throw new IllegalArgumentException("Negative plugCount of charger: " + id);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(ChargerSpecification copy) {
		Builder builder = new Builder();
		builder.id = copy.getId();
		builder.linkId = copy.getLinkId();
		builder.chargerType = copy.getChargerType();
		builder.plugPower = copy.getPlugPower();
		builder.plugCount = copy.getPlugCount();
		return builder;
	}

	@Override
	public Id<Charger> getId() {
		return id;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public String getChargerType() {
		return chargerType;
	}

	@Override
	public double getPlugPower() {
		return plugPower;
	}

	@Override
	public int getPlugCount() {
		return plugCount;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("linkId", linkId)
				.add("chargerType", chargerType)
				.add("plugPower", plugPower)
				.add("plugCount", plugCount)
				.toString();
	}

	public static final class Builder {
		private Id<Charger> id;
		private Id<Link> linkId;
		private String chargerType;
		private Double plugPower;
		private Integer plugCount;

		private Builder() {
		}

		public Builder id(Id<Charger> val) {
			id = val;
			return this;
		}

		public Builder linkId(Id<Link> val) {
			linkId = val;
			return this;
		}

		public Builder chargerType(String val) {
			chargerType = val;
			return this;
		}

		public Builder plugPower(double val) {
			plugPower = val;
			return this;
		}

		public Builder plugCount(int val) {
			plugCount = val;
			return this;
		}

		public ImmutableChargerSpecification build() {
			return new ImmutableChargerSpecification(this);
		}
	}
}
