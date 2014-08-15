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
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter;
import playground.boescpa.converters.vissim.tools.AmRouteConverter;
import playground.boescpa.converters.vissim.tools.MsRouteConverter;

import java.util.HashMap;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class MapRoutes {

	public static void main(String[] args) {
		String path2MsKeyMap = args[0];
		String path2AmKeyMap = args[1];
		String path2EventsFile = args[2];
		String path2MsNetwork = args[3];
		String path2VissimZoneShp = args[4];
		String path2AnmroutesFile = args[5];
		String path2VissimNetworkAnm = args[6];

		//HashMap<Id, Id[]> msKeyMap = PrepareNetworks.readKeyMaps(path2MsKeyMap);
		HashMap<Id, Id[]> amKeyMap = PrepareNetworks.readKeyMaps(path2AmKeyMap);

		//ConvEvents2Anm.RouteConverter msRouteConverter = new MsRouteConverter();
		//HashMap<Id, Long[]> msRoutes = msRouteConverter.convert(msKeyMap, path2EventsFile, path2MsNetwork, path2VissimZoneShp);
		ConvEvents2Anm.RouteConverter amRouteConverter = new AmRouteConverter();
		HashMap<Id, Long[]> amRoutes = amRouteConverter.convert(amKeyMap, path2AnmroutesFile, path2VissimNetworkAnm, path2VissimZoneShp);
	}

}
