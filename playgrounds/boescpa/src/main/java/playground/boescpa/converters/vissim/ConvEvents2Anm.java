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

import org.matsim.api.core.v01.Id;

import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;
import playground.boescpa.converters.vissim.tools.AmNetworkMapper;
import playground.boescpa.converters.vissim.tools.AmRouteConverter;
import playground.boescpa.converters.vissim.tools.MsNetworkMapper;
import playground.boescpa.converters.vissim.tools.MsRouteConverter;

/**
 * Extends and implements the abstract class ConvEvents for Anm-Files.
 *
 * @author boescpa
 */
public class ConvEvents2Anm extends ConvEvents {

	public ConvEvents2Anm(BaseGridCreator baseGridCreator, NetworkMapper matsimNetworkMapper, NetworkMapper anmNetworkMapper, RouteConverter matsimRouteConverter, RouteConverter anmRouteConverter, TripMatcher tripMatcher) {
		super(baseGridCreator, matsimNetworkMapper, anmNetworkMapper, matsimRouteConverter, anmRouteConverter, tripMatcher);
	}

	public static void main(String[] args) {
		// path2VissimZoneShp = args[0];
		// path2MATSimNetwork = args[1];
		// path2VissimNetwork = args[2];
		// path2EventsFile = args[3];
		// path2VissimRoutesFile = args[4];
		// path2NewVissimRoutesFile = args[5];
		// scaleFactor = Integer.parseInt(args[6]);

		ConvEvents convEvents = createDefaultConvEvents2Anm();
		convEvents.convert(args);
	}

	public static ConvEvents2Anm createDefaultConvEvents2Anm() {
		return new ConvEvents2Anm(new playground.boescpa.converters.vissim.tools.BaseGridCreator(), new MsNetworkMapper(), new AmNetworkMapper(),
				new MsRouteConverter(), new AmRouteConverter(), new playground.boescpa.converters.vissim.tools.TripMatcher());
	}

	@Override
	public void writeRoutes(HashMap<Id<Trip>, Integer> demandPerVissimTrip, String path2AnmRoutesFile, String path2NewAnmRoutesFile) {
	}
}
