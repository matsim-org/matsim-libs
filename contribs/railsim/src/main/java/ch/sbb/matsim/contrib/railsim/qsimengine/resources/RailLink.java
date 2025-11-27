/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package ch.sbb.matsim.contrib.railsim.qsimengine.resources;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.vehicles.VehicleType;

import java.util.Objects;
import java.util.Set;

/**
 * Rail links can have multiple tracks and corresponds to exactly one link.
 */
public final class RailLink implements HasLinkId {
	private final Id<Link> id;
	private final boolean isEntryLink;
	private final boolean isExitLink;
	private final boolean isNonBlockingArea;

	/**
	 * Id of opposite link, if any.
	 */
	@Nullable
	private final Id<Link> oppositeLinkId;

	@Nullable
	private final Set<Id<Link>> disallowedNextLinks;

	public final double length;
	public final double minimumHeadwayTime;

	final int tracks;

	private double freeSpeed;

	/**
	 * Maximum speed by vehicle type, which overrides the free speed if set.
	 */
	private final Object2DoubleMap<Id<VehicleType>> vMax;

	/**
	 * Resource this link belongs to.
	 */
	RailResourceInternal resource;

	public RailLink(Link link, Link opposite) {
		this(link, opposite, Object2DoubleMap.ofEntries(), null);
	}

	public RailLink(Link link, Link opposite, Object2DoubleMap<Id<VehicleType>> vMax, Set<Id<Link>> disallowedNextLinks) {
		this.id = link.getId();
		this.length = link.getLength();
		this.tracks = RailsimUtils.getTrainCapacity(link);
		this.freeSpeed = link.getFreespeed();
		this.minimumHeadwayTime = RailsimUtils.getMinimumHeadwayTime(link);
		this.isEntryLink = RailsimUtils.isEntryLink(link);
		this.isExitLink = RailsimUtils.isExitLink(link);
		this.isNonBlockingArea = RailsimUtils.isLinkNonBlockingArea(link);
		this.oppositeLinkId = opposite != null ? opposite.getId() : null;
		this.vMax = vMax;
		this.disallowedNextLinks = disallowedNextLinks;
	}

	@Override
	public Id<Link> getLinkId() {
		return id;
	}

	void setResource(RailResourceInternal resource) {
		this.resource = resource;
	}

	/**
	 * Access to the underlying resource.
	 */
	public RailResource getResource() {
		return resource;
	}

	/**
	 * Returns the allowed freespeed, depending on the context, which is given via driver.
	 */
	public double getAllowedFreespeed(MobsimDriverAgent driver) {
		VehicleType type = driver.getVehicle().getVehicle().getType();

		double vMax = this.vMax.getOrDefault(type.getId(), -1);
		if (vMax > -1)
			return Math.min(vMax, type.getMaximumVelocity());

		return Math.min(getFreeSpeed(), type.getMaximumVelocity());
	}

	/**
	 * Entry link of a station relevant for re-routing.
	 */
	public boolean isEntryLink() {
		return isEntryLink;
	}

	/**
	 * Exit link of a station for re-routing.
	 */
	public boolean isExitLink() {
		return isExitLink;
	}

	/**
	 * Whether this link is a non-blocking area, e.g. an intersection area.
	 */
	public boolean isNonBlockingArea() {
		return isNonBlockingArea;
	}

	/**
	 * Length in meter.
	 */
	public double getLength() {
		return length;
	}

	/**
	 * Determines if the given link is the opposite link of this link.
	 *
	 * @param linkId the identifier of the link to be checked
	 * @return true if the provided linkId matches the opposite link's Id, false otherwise
	 */
	public boolean isOppositeLink(Id<Link> linkId) {
		return oppositeLinkId != null && oppositeLinkId.equals(linkId);
	}

	/**
	 * Determines if the given link is disallowed to be used as the next link.
	 */
	public boolean isDisallowedNextLink(Id<Link> linkId) {
		return disallowedNextLinks != null && disallowedNextLinks.contains(linkId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RailLink link = (RailLink) o;
		return Objects.equals(id, link.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "RailLink{" +
			"id=" + id +
			", resource=" + resource +
			'}';
	}

	public double getFreeSpeed() {
		return freeSpeed;
	}

	public void setFreeSpeed(double freeSpeed) {
		this.freeSpeed = freeSpeed;
	}
}
