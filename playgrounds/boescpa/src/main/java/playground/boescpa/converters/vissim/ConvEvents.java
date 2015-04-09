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

package playground.boescpa.converters.vissim;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import playground.boescpa.converters.vissim.tools.AbstractRouteConverter;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;

/**
 * Provides the environment to convert MATSim-Events to an ANMRoutes file importable in VISSIM.
 *
 * @author boescpa
 */
public abstract class ConvEvents {

	private final BaseGridCreator baseGridCreator;
	private final NetworkMapper matsimNetworkMapper;
	private final NetworkMapper vissimNetworkMapper;
	private final RouteConverter matsimRouteConverter;
	private final RouteConverter vissimRouteConverter;
	private final TripMatcher tripMatcher;

	public ConvEvents() {
		this.baseGridCreator = null;
		this.matsimNetworkMapper = null;
		this.vissimNetworkMapper = null;
		this.matsimRouteConverter = null;
		this.vissimRouteConverter = null;
		this.tripMatcher = null;
	}

	public ConvEvents(BaseGridCreator baseGridCreator, NetworkMapper matsimNetworkMapper, NetworkMapper vissimNetworkMapper,
					  RouteConverter matsimRouteConverter, RouteConverter vissimRouteConverter, TripMatcher tripMatcher) {
		this.baseGridCreator = baseGridCreator;
		this.matsimNetworkMapper = matsimNetworkMapper;
		this.vissimNetworkMapper = vissimNetworkMapper;
		this.matsimRouteConverter = matsimRouteConverter;
		this.vissimRouteConverter = vissimRouteConverter;
		this.tripMatcher = tripMatcher;
	}

	public void convert(String[] args) {
		if (args.length != 7) {
			throw new IllegalArgumentException("Wrong number of input arguments.");
		}

		String path2VissimZoneShp = args[0];
		String path2MATSimNetwork = args[1];
		String path2VissimNetwork = args[2];
		String path2EventsFile = args[3];
		String path2VissimRoutesFile = args[4];
		String path2NewVissimRoutesFile = args[5];
		int scaleFactor = Integer.parseInt(args[6]);

		Network mutualBaseGrid = this.baseGridCreator.createMutualBaseGrid(path2VissimZoneShp);
		HashMap<Id<Link>, Id<Node>[]> keyMsNetwork = this.matsimNetworkMapper.mapNetwork(path2MATSimNetwork, mutualBaseGrid, path2VissimZoneShp);
		HashMap<Id<Link>, Id<Node>[]> keyAmNetwork = this.vissimNetworkMapper.mapNetwork(path2VissimNetwork, mutualBaseGrid, "");
		List<HashMap<Id<Trip>, Long[]>> msTripsCol = this.matsimRouteConverter.convert(keyMsNetwork, path2EventsFile, path2MATSimNetwork, path2VissimZoneShp);
		List<HashMap<Id<Trip>, Long[]>> amTripsCol = this.vissimRouteConverter.convert(keyAmNetwork, path2VissimRoutesFile, "", "");

		int hourCounter = 0;
		for (HashMap<Id<Trip>, Long[]> msTrips : msTripsCol) {
			for (HashMap<Id<Trip>, Long[]> amTrips : amTripsCol) {
				HashMap<Id<Trip>, Integer> demandPerAnmTrip = this.tripMatcher.matchTrips(msTrips, amTrips);
				demandPerAnmTrip = scaleDemandByFactor(demandPerAnmTrip, scaleFactor);
				String newPath2NewVissimRoutesFile = insertVersNumInFilepath(path2NewVissimRoutesFile, hourCounter);
				writeRoutes(demandPerAnmTrip, path2VissimRoutesFile, newPath2NewVissimRoutesFile);
				hourCounter++;
			}
		}
	}

	private HashMap<Id<Trip>, Integer> scaleDemandByFactor(HashMap<Id<Trip>, Integer> demandPerAnmTrip, int scaleFactor) {
		HashMap<Id<Trip>, Integer> scaledDemandPerAnmTrip = new HashMap<>();
		for (Id<Trip> tripId : demandPerAnmTrip.keySet()) {
			scaledDemandPerAnmTrip.put(tripId, demandPerAnmTrip.get(tripId)*scaleFactor);
		}
		return scaledDemandPerAnmTrip;
	}

	public static String insertVersNumInFilepath(String path2File, int versNum) {
		String[] subPath = path2File.split("\\.");
		String newPath = "";
		for (int i = 0; i < subPath.length - 1; i++) {
			newPath = newPath + subPath[i];
		}
		newPath = newPath + "_" + String.valueOf(versNum) + "." + subPath[subPath.length - 1];
		return newPath;
	}

	/**
	 * Rewrite Vissim file with new demand numbers for each Vissim-Route
	 *
	 * @param demandPerVissimTrip
	 * @param path2VissimRoutesFile
	 * @param path2NewVissimRoutesFile At the specified location a new VissimRoutes-File will be created. It is an exact copy
	 *                        of the given VissimFile except for the demands stated at the routes. These are the new
	 *                        demands given in demandPerVissimTrip.
	 */
	public abstract void writeRoutes(HashMap<Id<Trip>, Integer> demandPerVissimTrip, String path2VissimRoutesFile, String path2NewVissimRoutesFile);

	public interface BaseGridCreator {

		/**
		 * Create mutual base grid.
		 *
		 * @param path2VissimZoneShp
		 * @return A new data set (nodes) which represents both input networks jointly.
		 */
		Network createMutualBaseGrid(String path2VissimZoneShp);

	}

	public interface NetworkMapper {

		/**
		 * Creates a key that maps the provided network (links) to the mutual base grid.
		 * If the network is larger than the zones provided the network is cut.
		 *
		 * @param path2Network
		 * @param mutualBaseGrid
		 * @param path2VissimZoneShp
		 * @return The key that matches the network (links) to the base grid.
		 */
		HashMap<Id<Link>,Id<Node>[]> mapNetwork(String path2Network, Network mutualBaseGrid, String path2VissimZoneShp);

	}

	public interface RouteConverter {

		/**
		 * Convert given routes to trips on the common base grid.
		 *
		 * @param networkKey	Key that matches the original network to the common base grid.
		 * @param path2RouteFile	Path to a file that provides the routes (eg. as matsim events or as anmroutes).
		 * @param path2OrigNetwork	The original network the routes were created on.
		 * @param path2VissimZoneShp
		 * @return	List of trip collections. Each entry of the list represents one hour simulation time. If there is
		 * 			only one entry, no time was provided for the trips (e.g. the case for vissim trips).
		 */
		List<HashMap<Id<Trip>, Long[]>> convert(HashMap<Id<Link>, Id<Node>[]> networkKey, String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp);
	}

	public interface TripMatcher {

		/**
		 * Find for each MATSim-trip a best match in the ANM-trips
		 *
		 * @param msTrips
		 * @param amTrips
		 * @return A HashMap having for each ANM-trip (Id) the number (Integer) of MATSim-trips which were found to
		 * 			to match this ANM-trip best.
		 */
		HashMap<Id<Trip>,Integer> matchTrips(HashMap<Id<Trip>, Long[]> msTrips, HashMap<Id<Trip>, Long[]> amTrips);
	}
}
