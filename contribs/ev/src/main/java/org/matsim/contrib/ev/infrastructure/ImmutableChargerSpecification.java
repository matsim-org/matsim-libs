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
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

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
	private final Attributes attributes;

	private ImmutableChargerSpecification( ChargerSpecificationBuilder builder ) {
		id = Objects.requireNonNull(builder.id);
		linkId = Objects.requireNonNull(builder.linkId);
		chargerType = Objects.requireNonNull(builder.chargerType);
		plugPower = Objects.requireNonNull(builder.plugPower);
		plugCount = Objects.requireNonNull(builder.plugCount);
		attributes = builder.attributes != null ? builder.attributes : new AttributesImpl();

		Preconditions.checkArgument(plugPower >= 0, "Negative plugPower of charger: %s", id);
		Preconditions.checkArgument(plugCount >= 0, "Negative plugCount of charger: %s", id);
	}

	public static ChargerSpecificationBuilder newBuilder() {
		return new ChargerSpecificationBuilder();
	}

	public static ChargerSpecificationBuilder newBuilder( ChargerSpecification copy ) {
		ChargerSpecificationBuilder builder = new ChargerSpecificationBuilder();
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
	public Attributes getAttributes() {
		return attributes;
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

	public static final class ChargerSpecificationBuilder{
		private Id<Charger> id;
		private Id<Link> linkId;
		private String chargerType;
		private Double plugPower;
		private Integer plugCount;
		private Attributes attributes;

		private ChargerSpecificationBuilder() {
		}

		public ChargerSpecificationBuilder id( Id<Charger> val ) {
			id = val;
			return this;
		}

		public ChargerSpecificationBuilder linkId( Id<Link> val ) {
			linkId = val;
			return this;
		}

		public ChargerSpecificationBuilder chargerType( String val ) {
			chargerType = val;
			return this;
		}

		public ChargerSpecificationBuilder plugPower( double val ) {
			plugPower = val;
			return this;
		}

		public ChargerSpecificationBuilder plugCount( int val ) {
			plugCount = val;
			return this;
		}

		public ChargerSpecificationBuilder attributes( Attributes val ) {
			attributes = val;
			return this;
		}

		public ImmutableChargerSpecification build() {
			return new ImmutableChargerSpecification(this);
		}
	}
}
