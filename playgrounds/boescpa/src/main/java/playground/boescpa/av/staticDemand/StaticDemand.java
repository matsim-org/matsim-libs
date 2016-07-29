/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import playground.boescpa.analysis.trips.Trip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * WHAT IS IT FOR?
 *
 * @author boescpa
 */
public abstract class StaticDemand {
	protected static Logger log = Logger.getLogger(StaticDemand.class);

	public abstract List<Trip> getOriginalDemand();

	public abstract List<Trip> getFilteredDemand();

	public abstract List<Trip> getSortedDemand();

	public static List<Id> getAllAgents(List<Trip> modeFilteredTrips) {
		log.info("Filter demand for agents...");
		List<Id> agents = new ArrayList<>();
		Set<Id> agentsSet = new HashSet<>();
		for (Trip trip : modeFilteredTrips) {
			if (!agentsSet.contains(trip.agentId)) {
				agentsSet.add(trip.agentId);
				agents.add(trip.agentId);
			}
		}
		log.info("Filter demand for agents... done.");
		log.info("Total " + agents.size() + " driving agents found.");
		return agents;
	}

}
