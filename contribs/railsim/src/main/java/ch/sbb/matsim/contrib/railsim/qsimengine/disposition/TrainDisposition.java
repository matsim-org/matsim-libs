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

package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailLink;
import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTransitDriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Disposition, handling route and track reservations.
 */
public interface TrainDisposition {

	/**
	 * Method invoked when a train is departing.
	 */
	void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route);

	/**
	 * Called by the driver when an entry link is within stop distance.
	 *
	 * @param segment the original link segment between entry and exit
	 * @return the route change, or null if nothing should be changed
	 */
	@Nullable
	default List<RailLink> requestRoute(double time, RailsimTransitDriverAgent driver, List<RailLink> segment,
										RailLink entry, RailLink exit) {
		return null;
	}

	/**
	 * Train is reaching the given links and is trying to block them.
	 *
	 * @return links of the request that are exclusively blocked for the train.
	 */
	List<RailLink> blockRailSegment(double time, MobsimDriverAgent driver, List<RailLink> segment);

	/**
	 * Inform the resource manager that the train has passed a link that can now be unblocked.
	 * This needs to be called after track states have been updated already.
	 */
	void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link);

}
