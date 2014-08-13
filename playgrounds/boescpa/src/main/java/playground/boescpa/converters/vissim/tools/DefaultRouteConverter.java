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
public class DefaultRouteConverter implements ConvEvents2Anm.RouteConverter {
	@Override
	public HashMap<Id, Long[]> convertEvents(HashMap<Id, Id[]> keyMsNetwork, String path2EventsFile, String path2VissimZoneShp) {
		List<Trip> trips = events2Trips(path2EventsFile, path2VissimZoneShp);
		return trips2Routes(trips, keyMsNetwork);
	}

	private List<Trip> events2Trips(String path2EventsFile, String path2VissimZoneShp) {
		List<Trip> trips = new ArrayList<Trip>();
		// Go trough events and look at all linkleaveevents in area and mode car (and all arrivalevents).
		// Store events in a hashmap with agent as key and arraylist of links as value.
		// 	if for an agent no entry, create new entry = start new trip...
		//	if arrivalevent and agent found in current trips, transform trip into new trip without agent, start time, end time, route (array of links).
		//		then remove trip from active trip hashmap.
		// This should result in a set of trips (start times, end times, array of links).
		return trips;
	}

	@Override
	public HashMap<Id, Long[]> convertRoutes(HashMap<Id, Id[]> keyAmNetwork, String path2AnmroutesFile) {
		List<Trip> trips = anmroutes2Trips(path2AnmroutesFile);
		return trips2Routes(trips, keyAmNetwork);
	}

	private List<Trip> anmroutes2Trips(String path2AnmroutesFile) {
		return null;
	}

	private HashMap<Id, Long[]> trips2Routes(List<Trip> trips, HashMap<Id, Id[]> keyMsNetwork) {
		// Transform all trips into a set of trips (start times, end times, array of keys) with the keyNetwork.
		return null;
	}

	private class Trip {
		final double startTime;
		final double endTime;
		final List<Id> links;

		Trip(double startTime, double endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.links = new ArrayList<Id>();
		}
	}

}
