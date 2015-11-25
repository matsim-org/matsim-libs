/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.converters.visum.preprocessing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;
import playground.boescpa.analysis.trips.TripHandler;
import playground.boescpa.analysis.trips.TripProcessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Extends the abstract TripProcessor with a visum-specific implementation.
 *
 * @author boescpa
 */
public class VisumTripProcessor extends TripProcessor {

	// TODO-boescpa Write tests...

	public VisumTripProcessor(String tripFile, SpatialCutter spatialCutter) {
		super(tripFile, spatialCutter);
	}

	/**
	 * Check for all found trips if they have failed. If so, remove them.
	 *
	 * @return HashMap with one Element (key (String): "failedAgents", value (ArrayList<Id>): the ids of the failed agents).
	 */
	@Override
	public HashMap<String, Object> analyzeTrips(TripHandler tripData, Network network) {

		ArrayList<Id> failedAgents = new ArrayList<Id>();

		// remove failed trips
		for (Id personId : tripData.getStartLink().keySet()) {
			if (!personId.toString().contains("pt")) {
				Iterator[] trips = {
						tripData.getStartLink().getValues(personId).iterator(),
						tripData.getPurpose().getValues(personId).iterator(),
						tripData.getEndLink().getValues(personId).iterator(),
						tripData.getStartTime().getValues(personId).iterator(),
						tripData.getMode().getValues(personId).iterator(),
						tripData.getPath().getValues(personId).iterator(),
						tripData.getEndTime().getValues(personId).iterator()};

				while (trips[0].hasNext()) {
					// move all iterators one further
					Id agentId = (Id) trips[0].next();
					String purpose = (String) trips[1].next();
					Id endLink = (Id) trips[2].next();
					for (int i = 3; i < trips.length; i++) {
						trips[i].next();
					}
					// check if failed trip
					if ((network.getLinks().get(endLink) == null) ||
							purpose.equals("null") ||
							purpose.equals("stuck")) {
						// and if so, remove it
						for (Iterator it : trips) {
							it.remove();
						}
						failedAgents.add(agentId);
					}
				}
			}
		}

		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("failedAgents", failedAgents);
		return result;
	}
}
