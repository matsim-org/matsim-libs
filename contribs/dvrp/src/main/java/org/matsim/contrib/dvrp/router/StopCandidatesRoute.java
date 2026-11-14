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

import java.util.Optional;

import org.matsim.api.core.v01.population.Route;

/**
 * A route of a dvrp main leg that can carry the {@link StopCandidates} of its trip. {@link DvrpRoutingModule} attaches
 * them if the {@link DvrpRoutingModule.AccessEgressFacilityFinder} returns alternatives.
 *
 * @author nkuehnel / MOIA
 */
public interface StopCandidatesRoute extends Route {
	Optional<StopCandidates> getStopCandidates();

	void setStopCandidates(StopCandidates stopCandidates);
}
