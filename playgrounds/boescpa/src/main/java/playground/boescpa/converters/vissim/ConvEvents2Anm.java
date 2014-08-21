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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import playground.boescpa.converters.vissim.tools.*;

import java.util.HashMap;

/**
 * Provides the environment to convert MATSim-Events to an ANMRoutes file importable in VISSIM.
 *
 * @author boescpa
 */
public class ConvEvents2Anm {

	private final BaseGridCreator baseGridCreator;
	private final NetworkMapper matsimNetworkMapper;
	private final NetworkMapper anmNetworkMapper;
	private final RouteConverter matsimRouteConverter;
	private final RouteConverter anmRouteConverter;
	private final TripMatcher tripMatcher;

	public ConvEvents2Anm(BaseGridCreator baseGridCreator, NetworkMapper matsimNetworkMapper, NetworkMapper anmNetworkMapper,
						  RouteConverter matsimRouteConverter, RouteConverter anmRouteConverter, TripMatcher tripMatcher) {
		this.baseGridCreator = baseGridCreator;
		this.matsimNetworkMapper = matsimNetworkMapper;
		this.anmNetworkMapper = anmNetworkMapper;
		this.matsimRouteConverter = matsimRouteConverter;
		this.anmRouteConverter = anmRouteConverter;
		this.tripMatcher = tripMatcher;
	}

	public static void main(String[] args) {
		ConvEvents2Anm convEvents2Anm = createDefaultConvEvents2Anm();
		convEvents2Anm.convert(args);
	}

	public static ConvEvents2Anm createDefaultConvEvents2Anm() {
		return new ConvEvents2Anm(new playground.boescpa.converters.vissim.tools.BaseGridCreator(), new MsNetworkMapper(), new AmNetworkMapper(),
				new MsRouteConverter(), new AmRouteConverter(), new playground.boescpa.converters.vissim.tools.TripMatcher());
	}

	public void convert(String[] args) {
		String path2VissimZoneShp = args[0];
		String path2MATSimNetwork = args[1];
		String path2VissimNetworkAnm = args[2];
		String path2EventsFile = args[3];
		String path2AnmroutesFile = args[4];
		String path2NewAnmFile = args[5];

		Network mutualBaseGrid = this.baseGridCreator.createMutualBaseGrid(path2VissimZoneShp);
		HashMap<Id, Id[]> keyMsNetwork = this.matsimNetworkMapper.mapNetwork(path2MATSimNetwork, mutualBaseGrid, path2VissimZoneShp);
		HashMap<Id, Id[]> keyAmNetwork = this.anmNetworkMapper.mapNetwork(path2VissimNetworkAnm, mutualBaseGrid, "");
		HashMap<Id, Long[]> msTrips = this.matsimRouteConverter.convert(keyMsNetwork, path2EventsFile, path2MATSimNetwork, path2VissimZoneShp);
		HashMap<Id, Long[]> amTrips = this.anmRouteConverter.convert(keyAmNetwork, path2AnmroutesFile, "", "");
		HashMap<Id, Integer> demandPerAnmTrip = this.tripMatcher.matchTrips(msTrips, amTrips);

		// todo-boescpa Deal with start times of trips...

		writeAnmRoutes(demandPerAnmTrip, path2AnmroutesFile, path2NewAnmFile);
	}

	/**
	 * Rewrite ANMRoutes file with new demand numbers for each ANM-Route
	 *
	 * @param demandPerAnmTrip
	 * @param path2AnmFile
	 * @param path2NewAnmFile At the specified location a new ANMRoutes-File will be created. It is an exact copy
	 *                        of the given AnmFile except for the demands stated at the routes. These are the new
	 *                        demands given in demandPerAnmTrip.
	 */
	public void writeAnmRoutes(HashMap<Id, Integer> demandPerAnmTrip, String path2AnmFile, String path2NewAnmFile) {

	};

	public interface BaseGridCreator {

		/**
		 * Create mutual base grid.
		 *
		 * @param path2VissimZoneShp
		 * @return A new data set (nodes) which represents both input networks jointly.
		 */
		public Network createMutualBaseGrid(String path2VissimZoneShp);

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
		HashMap<Id,Id[]> mapNetwork(String path2Network, Network mutualBaseGrid, String path2VissimZoneShp);

	}

	public interface RouteConverter {

		/**
		 * Convert routes (in the form of events or anmroutes) to trips in on the common base grid.
		 *
		 * @param networkKey	Key that matches the original network to the common base grid.
		 * @param path2RouteFile	Path to a file that provides the routes (eg. as matsim events or as anmroutes).
		 * @param path2OrigNetwork	The original network the routes were created on.
		 * @param path2VissimZoneShp
		 * @return
		 */
		public HashMap<Id, Long[]> convert(HashMap<Id, Id[]> networkKey, String path2RouteFile, String path2OrigNetwork, String path2VissimZoneShp);
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
		HashMap<Id,Integer> matchTrips(HashMap<Id, Long[]> msTrips, HashMap<Id, Long[]> amTrips);
	}
}
