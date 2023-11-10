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
import ch.sbb.matsim.contrib.railsim.qsimengine.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import jakarta.inject.Inject;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple disposition without deadlock avoidance.
 */
public class SimpleDisposition implements TrainDisposition {

	private final RailResourceManager resources;
	private final TrainRouter router;

	@Inject
	public SimpleDisposition(RailResourceManager resources, TrainRouter router) {
		this.resources = resources;
		this.router = router;
	}

	@Override
	public void onDeparture(double time, MobsimDriverAgent driver, List<RailLink> route) {
		// Nothing to do.
	}

	@Override
	public DispositionResponse requestNextSegment(double time, TrainPosition position, List<RailLink> segment) {

		// Only re-routes if the link segment is occupied
//		for (RailLink link : segment) {
//			if (!resources.isBlockedBy(link, driver) && !resources.hasCapacity(link.getLinkId()))
//				return router.calcRoute(entry, exit);
//		}

		// This code was in rail engine, now needs to be part of disposition
		/*
			if (state.pt != null && RailsimCalc.considerReRouting(links, resources.getLink(state.headLink))) {

			int start = -1;
			int end = -1;
			RailLink entry = null;
			RailLink exit = null;

			for (int i = Math.max(0, state.routeIdx - 1); i < state.route.size(); i++) {
				RailLink l = state.route.get(i);

				if (l.isEntryLink()) {
					entry = l;
					start = i;
				} else if (start > -1 && l.isBlockedBy(state.driver)) {
					// check if any link beyond entry is already blocked
					// if that is the case re-route is not possible anymore
					break;
				} else if (start > -1 && l.isExitLink()) {
					exit = l;
					end = i;
					break;
				}
			}

			// there might be no exit link if this is the end of the route
			// exit will be set to null if re-route is too late
			// network could be wrong as well, but hard to verify
			if (exit != null) {
				....
			}
		 */

		// Assume rest of link is already reserved (fix block)
		double reserveDist = resources.getLink(position.getHeadLink()).getLength() - position.getHeadPosition();
		boolean stop = false;

		// Iterate all links that need to be blocked
		for (RailLink link : segment) {

			// Check if single link can be reserved
			if (resources.tryBlockTrack(time, position, link)) {
				reserveDist += link.getLength();
			} else {
				stop = true;
				break;
			}
		}

		return new DispositionResponse(reserveDist, stop, null);
	}

	@Override
	public void unblockRailLink(double time, MobsimDriverAgent driver, RailLink link) {

		// put resource handling into release track
		resources.releaseTrack(time, driver, link);
	}
}
