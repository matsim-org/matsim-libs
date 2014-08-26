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

package playground.boescpa.converters.vissim.tools;

import org.matsim.api.core.v01.Id;
import playground.boescpa.converters.vissim.ConvEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Maps the trips of a given routes-file with the given network keys.
 *
 * @author boescpa
 */
public abstract class AbstractRouteConverter implements ConvEvents.RouteConverter {

	@Override
	public HashMap<Id, Long[]> convert(HashMap<Id, Id[]> networkKey, String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp) {
		List<Trip> trips = routes2Trips(path2RouteFile, path2OrigNetwork, path2VissimZoneShp);
		return trips2SimpleRoutes(trips, networkKey);
	}

	protected abstract List<Trip> routes2Trips(String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp);

	private HashMap<Id, Long[]> trips2SimpleRoutes(List<Trip> trips, HashMap<Id, Id[]> keyMsNetwork) {
		HashMap<Id, Long[]> simpleRoutes = new HashMap<Id, Long[]>();

		for (Trip trip : trips) {
			ArrayList<Long> keyValsTrip = new ArrayList<Long>();
			for (Id link : trip.links) {
				Id[] keyValsLink = keyMsNetwork.get(link);
				// Jeweils ersten Wert von keyValsLink mit aktuell letzem Wert von keyValsTrip vergleichen und nur
				// hinzufügen, wenn unterschiedlich.
				int startIteration = 0;
				if (keyValsTrip.size() > 0) {
					if (keyValsTrip.get(keyValsTrip.size()-1) == Long.parseLong(keyValsLink[0].toString())) {
						startIteration = 1;
					}
				}
				// Ab zweitem Wert alle hinzufügen.
				for (int i = startIteration; i < keyValsLink.length; i++) {
					keyValsTrip.add(Long.parseLong(keyValsLink[i].toString()));
				}
			}
			simpleRoutes.put(trip.tripId, keyValsTrip.toArray(new Long[keyValsTrip.size()]));
		}

		return simpleRoutes;
	}

	final class Trip {
		final Id tripId;
		final double startTime;
		double endTime;
		final List<Id> links;

		Trip(Id tripId, double startTime) {
			this.tripId = tripId;
			this.startTime = startTime;
			this.endTime = 0;
			this.links = new ArrayList<Id>();
		}
	}
}
