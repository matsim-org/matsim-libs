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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.Objects;

/**
 * Rail links can have multiple tracks and corresponds to exactly one link.
 */
public final class RailLink implements HasLinkId {
	private final Id<Link> id;
	private final boolean isEntryLink;
	private final boolean isExitLink;

	public final double length;
	public final double minimumHeadwayTime;
	public final double freeSpeed;
	final int tracks;

	/**
	 * Resource this link belongs to.
	 */
	RailResourceInternal resource;

	public RailLink(Link link) {
		this.id = link.getId();
		this.length = link.getLength();
		this.tracks = RailsimUtils.getTrainCapacity(link);
		this.freeSpeed = link.getFreespeed();
		this.minimumHeadwayTime = RailsimUtils.getMinimumHeadwayTime(link);
		this.isEntryLink = RailsimUtils.isEntryLink(link);
		this.isExitLink = RailsimUtils.isExitLink(link);
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
		return Math.min(freeSpeed, driver.getVehicle().getVehicle().getType().getMaximumVelocity());
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
	 * Length in meter.
	 */
	public double getLength() {
		return length;
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
}
