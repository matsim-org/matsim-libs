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
import playground.boescpa.converters.vissim.ConvEvents2Anm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Maps the trips of a given events-file onto the given network
 * (network expected in the form of nodes representing a square grid).
 *
 * @author boescpa
 */
public abstract class AbstractRouteConverter implements ConvEvents2Anm.RouteConverter {

	@Override
	public HashMap<Id, Long[]> convert(HashMap<Id, Id[]> networkKey, String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp) {
		List<Trip> trips = routes2Trips(path2RouteFile, path2OrigNetwork, path2VissimZoneShp);
		return trips2Routes(trips, networkKey);
	}

	protected abstract List<Trip> routes2Trips(String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp);

	private HashMap<Id, Long[]> trips2Routes(List<Trip> trips, HashMap<Id, Id[]> keyMsNetwork) {
		// Transform all trips into a set of trips (start times, end times, array of keys) with the keyNetwork.
		return null;
	}

	final class Trip {
		final double startTime;
		double endTime;
		final List<Id> links;

		Trip(double startTime) {
			this.startTime = startTime;
			this.endTime = 0;
			this.links = new ArrayList<Id>();
		}
	}
}
