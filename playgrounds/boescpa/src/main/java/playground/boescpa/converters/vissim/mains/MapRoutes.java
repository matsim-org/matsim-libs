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

package playground.boescpa.converters.vissim.mains;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.ConvEvents2Anm;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;
import playground.boescpa.converters.vissim.tools.InpRouteConverter;
import playground.boescpa.converters.vissim.tools.MsRouteConverter;

/**
 * Creates routes from the matsim and the visum/vissim world and produces common representations of those.
 * The are written to the paths provided.
 * Provides also a method to read the routes back in from the files.
 *
 * @author boescpa
 */
public class MapRoutes {

	private static final String delimiter = ", ";

	public static void main(String[] args) {
		String path2MsKeyMap = args[0];
		String path2AmKeyMap = args[1];
		String path2EventsFile = args[2];
		String path2MsNetwork = args[3];
		String path2VissimZoneShp = args[4];
		String path2AnmroutesFile = args[5];
		String path2VissimNetworkAnm = args[6];
		String path2WriteMsRoutes = args[7];
		String path2WriteAmRoutes = args[8];
		String path2InpKeyMap = args[9];
		String path2InpFile = args[10];
		String path2WriteInpRoutes = args[11];

		// Create matsim routes:
		HashMap<Id<Link>, Id<Node>[]> msKeyMap = PrepareNetworks.readKeyMaps(path2MsKeyMap);
		ConvEvents2Anm.RouteConverter msRouteConverter = new MsRouteConverter();
		List<HashMap<Id<Trip>, Long[]>> msRoutes = msRouteConverter.convert(msKeyMap, path2EventsFile, path2MsNetwork, path2VissimZoneShp);
		for (int i = 0; i < msRoutes.size(); i++) {
			writeRoutes(msRoutes.get(i), ConvEvents.insertVersNumInFilepath(path2WriteMsRoutes, i));
		}

		/*
		// Create visum routes:
		HashMap<Id, Id[]> amKeyMap = PrepareNetworks.readKeyMaps(path2AmKeyMap);
		ConvEvents2Anm.RouteConverter amRouteConverter = new AmRouteConverter();
		List<HashMap<Id, Long[]>> amRoutes = amRouteConverter.convert(amKeyMap, path2AnmroutesFile, path2VissimNetworkAnm, path2VissimZoneShp);
		for (int i = 0; i < amRoutes.size(); i++) {
			writeRoutes(amRoutes.get(i), ConvEvents.insertVersNumInFilepath(path2WriteAmRoutes, i));
		}
		*/

		// Create visim routes:
		HashMap<Id<Link>, Id<Node>[]> inpKeyMap = PrepareNetworks.readKeyMaps(path2InpKeyMap);
		ConvEvents.RouteConverter inpRouteConverter = new InpRouteConverter();
		List<HashMap<Id<Trip>, Long[]>> inpRoutes = inpRouteConverter.convert(inpKeyMap, path2InpFile, "", "");
		for (int i = 0; i < inpRoutes.size(); i++) {
			writeRoutes(inpRoutes.get(i), ConvEvents.insertVersNumInFilepath(path2WriteInpRoutes, i));
		}
	}

	public static void writeRoutes(HashMap<Id<Trip>, Long[]> routes, String path2WriteRoutes) {
		try {
			final String header = "RouteId, LinkIds...";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteRoutes);
			out.write(header); out.newLine();
			for (Id<Trip> routeId : routes.keySet()) {
				String line = routeId.toString();
				for (Long linkId : routes.get(routeId)) {
					line = line + delimiter + linkId.toString();
				}
				out.write(line); out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Writing of " + path2WriteRoutes + " failed.");
			e.printStackTrace();
		}
	}

	public static HashMap<Id<Trip>, Long[]> readRoutes(String path2RoutesFile) {
		HashMap<Id<Trip>, Long[]> routes = new HashMap<>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2RoutesFile);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] route = line.split(delimiter);
				String routeId = route[0];
				Long[] linkIds = new Long[route.length-1];
				for (int i = 1; i < route.length; i++) {
					linkIds[i-1] = Long.parseLong(route[i]);
				}
				routes.put(Id.create(routeId, Trip.class), linkIds);
				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2RoutesFile + " failed.");
			e.printStackTrace();
		}
		return routes;
	}
}
