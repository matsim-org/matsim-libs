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

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.InpRouteConverter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

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

		/*
		// Create matsim routes:
		HashMap<Id, Id[]> msKeyMap = PrepareNetworks.readKeyMaps(path2MsKeyMap);
		ConvEvents2Anm.RouteConverter msRouteConverter = new MsRouteConverter();
		HashMap<Id, Long[]> msRoutes = msRouteConverter.convert(msKeyMap, path2EventsFile, path2MsNetwork, path2VissimZoneShp);
		writeRoutes(msRoutes, path2WriteMsRoutes);
		*/

		/*
		// Create visum routes:
		HashMap<Id, Id[]> amKeyMap = PrepareNetworks.readKeyMaps(path2AmKeyMap);
		ConvEvents2Anm.RouteConverter amRouteConverter = new AmRouteConverter();
		HashMap<Id, Long[]> amRoutes = amRouteConverter.convert(amKeyMap, path2AnmroutesFile, path2VissimNetworkAnm, path2VissimZoneShp);
		writeRoutes(amRoutes, path2WriteAmRoutes);
		*/

		// Create visim routes:
		HashMap<Id, Id[]> inpKeyMap = PrepareNetworks.readKeyMaps(path2InpKeyMap);
		ConvEvents.RouteConverter inpRouteConverter = new InpRouteConverter();
		HashMap<Id, Long[]> inpRoutes = inpRouteConverter.convert(inpKeyMap, path2InpFile, "", "");
		writeRoutes(inpRoutes, path2WriteInpRoutes);
	}

	public static void writeRoutes(HashMap<Id, Long[]> routes, String path2WriteRoutes) {
		try {
			final String header = "RouteId, LinkIds...";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteRoutes);
			out.write(header); out.newLine();
			for (Id routeId : routes.keySet()) {
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

	public static HashMap<Id, Long[]> readRoutes(String path2RoutesFile) {
		HashMap<Id, Long[]> routes = new HashMap<Id, Long[]>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2RoutesFile);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] route = line.split(delimiter);
				Id routeId = new IdImpl(route[0]);
				Long[] linkIds = new Long[route.length-1];
				for (int i = 1; i < route.length; i++) {
					linkIds[i-1] = Long.parseLong(route[i]);
				}
				routes.put(routeId, linkIds);
				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2RoutesFile + " failed.");
			e.printStackTrace();
		}
		return routes;
	}
}
