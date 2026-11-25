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

import org.matsim.facilities.Facility;

import com.google.common.base.Preconditions;

/**
 * Access and egress facilities (e.g. stops) a passenger accepts for one trip. The first entry of each list is the
 * preferred facility, i.e. the one a finder would return as the single access/egress facility (see
 * {@link DvrpRoutingModule.AccessEgressFacilityFinder#findFacilities}); the other entries are alternatives.
 *
 * @author nkuehnel / MOIA
 */
public record AccessEgressFacilityCandidates(List<Facility> access, List<Facility> egress) {
	public AccessEgressFacilityCandidates {
		access = List.copyOf(Objects.requireNonNull(access, "access"));
		egress = List.copyOf(Objects.requireNonNull(egress, "egress"));
		Preconditions.checkArgument(!access.isEmpty(), "at least one access facility is required");
		Preconditions.checkArgument(!egress.isEmpty(), "at least one egress facility is required");
	}

	public static AccessEgressFacilityCandidates of(Facility access, Facility egress) {
		return new AccessEgressFacilityCandidates(List.of(access), List.of(egress));
	}

	public Facility preferredAccess() {
		return access.getFirst();
	}

	public Facility preferredEgress() {
		return egress.getFirst();
	}

	/**
	 * @return true if there is more than the preferred facility on at least one side
	 */
	public boolean hasAlternatives() {
		return access.size() > 1 || egress.size() > 1;
	}
}
