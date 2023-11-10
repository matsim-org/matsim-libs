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

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Fixed block where each track is exclusively reserved by one agent.
 */
public final class FixedBlockResource implements RailResource {

	/**
	 * Links belonging to this resource.
	 */
	final List<RailLink> links;

	/**
	 * Agents holding this resource exclusively.
	 */
	final Set<MobsimDriverAgent> reservations;

	/**
	 * Maximum number of reservations.
	 */
	int capacity;

	public FixedBlockResource(List<RailLink> links) {
		this.links = links;
		this.reservations = new HashSet<>();
		this.capacity = links.stream().mapToInt(RailLink::getNumberOfTracks).min().orElseThrow();
	}

	@Override
	public List<RailLink> getLinks() {
		return links;
	}

	/**
	 * Whether an agent is able to block this resource.
	 */
	@Override
	public boolean hasCapacity(double time, TrainPosition position) {
		return reservations.size() < capacity;
	}

	@Override
	public boolean isReservedBy(MobsimDriverAgent driver) {
		return reservations.contains(driver);
	}

	@Override
	public void reserve(TrainPosition position) {
		reservations.add(position.getDriver());
	}

	@Override
	public void release(MobsimDriverAgent driver) {
		reservations.remove(driver);
	}

}
