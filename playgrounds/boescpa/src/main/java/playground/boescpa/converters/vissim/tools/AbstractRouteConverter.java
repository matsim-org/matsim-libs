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
	public List<HashMap<Id, Long[]>> convert(HashMap<Id, Id[]> networkKey, String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp) {
		List<Trip> trips = routes2Trips(path2RouteFile, path2OrigNetwork, path2VissimZoneShp);
		return trips2SimpleRoutes(trips, networkKey);
	}

	protected abstract List<Trip> routes2Trips(String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp);

	private List<HashMap<Id, Long[]>> trips2SimpleRoutes(List<Trip> trips, HashMap<Id, Id[]> keyMsNetwork) {
		List<HashMap<Id, Long[]>> routes = new ArrayList<HashMap<Id, Long[]>>();

		final int lastStartTime = (int)Math.floor(findLastStartTime(trips));
		for (int i = 0; i <= lastStartTime; i++) {
			HashMap<Id, Long[]> simpleRoutes = new HashMap<Id, Long[]>();
			routes.add(simpleRoutes);
			for (Trip trip : trips) {
				if (trip.startTime < i+1 && trip.endTime >= i) {
					ArrayList<Long> keyValsTrip = new ArrayList<Long>();
					for (Id link : trip.links) {
						Id[] keyValsLink = keyMsNetwork.get(link);
						// Each time compare first value of keyValsLink with presently last value of keyValsTrip.
						// Adding only if actually different.
						int startIteration = 0;
						if (keyValsTrip.size() > 0) {
							if (keyValsTrip.get(keyValsTrip.size() - 1) == Long.parseLong(keyValsLink[0].toString())) {
								startIteration = 1;
							}
						}
						// From the second value add all.
						for (int j = startIteration; j < keyValsLink.length; j++) {
							keyValsTrip.add(Long.parseLong(keyValsLink[j].toString()));
						}
					}
					simpleRoutes.put(trip.tripId, keyValsTrip.toArray(new Long[keyValsTrip.size()]));
				}
			}
		}

		return routes;
	}

	private double findLastStartTime(List<Trip> trips) {
		double lastStartTime = 0;
		for (Trip trip : trips) {
			if (trip.startTime > lastStartTime) {
				lastStartTime = trip.startTime;
			}
		}
		return lastStartTime;
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
