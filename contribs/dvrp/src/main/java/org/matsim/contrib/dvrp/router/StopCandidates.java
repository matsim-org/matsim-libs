/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import com.google.common.base.Preconditions;

/**
 * Routed access and egress stop candidates of a trip, as computed by {@link DvrpRoutingModule}: for each candidate the
 * stop and the walk between the passenger's origin/destination and the stop. The first entry of each list is the
 * preferred stop, i.e. the stop of the routed trip; the other entries are alternatives.
 * <p>
 * The walk durations and distances are those of the access/egress routing modules, so that a trip rebuilt for another
 * candidate (e.g. after a booking) can use exactly the same values.
 *
 * @author nkuehnel / MOIA
 */
public record StopCandidates(List<Candidate> access, List<Candidate> egress) {

	/**
	 * @param linkId       link of the stop
	 * @param coord        coordinate of the stop (for the interaction activity)
	 * @param walkDuration walk duration between the origin/destination and the stop [s]
	 * @param walkDistance walk distance between the origin/destination and the stop [m]
	 */
	public record Candidate(Id<Link> linkId, Coord coord, double walkDuration, double walkDistance) {
		public Candidate {
			Objects.requireNonNull(linkId, "linkId");
			Objects.requireNonNull(coord, "coord");
			Preconditions.checkArgument(Double.isFinite(walkDuration) && walkDuration >= 0,
					"walkDuration must be finite and non-negative: %s", walkDuration);
			Preconditions.checkArgument(Double.isFinite(walkDistance) && walkDistance >= 0,
					"walkDistance must be finite and non-negative: %s", walkDistance);
		}
	}

	public StopCandidates {
		access = List.copyOf(Objects.requireNonNull(access, "access"));
		egress = List.copyOf(Objects.requireNonNull(egress, "egress"));
		Preconditions.checkArgument(!access.isEmpty(), "at least one access candidate is required");
		Preconditions.checkArgument(!egress.isEmpty(), "at least one egress candidate is required");
	}

	public Candidate preferredAccess() {
		return access.getFirst();
	}

	public Candidate preferredEgress() {
		return egress.getFirst();
	}

	public boolean hasAlternatives() {
		return access.size() > 1 || egress.size() > 1;
	}
}
