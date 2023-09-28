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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * A resource representing multiple {@link RailLink}.
 */
public class RailResource {

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

	public RailResource(List<RailLink> links) {
		this.links = links;
		this.reservations = new HashSet<>();
		this.capacity = links.stream().mapToInt(RailLink::getNumberOfTracks).min().orElseThrow();
	}

	/**
	 * Whether an agent is able to block this resource.
	 */
	boolean hasCapacity() {
		return reservations.size() < capacity;
	}

}
