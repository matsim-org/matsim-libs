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

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.ConvEvents2Inp;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;

/**
 * Creates a new inp-file exchanging the demand (relative and absolute) in the given inp-file.
 *
 * @author boescpa
 */
public class ConvEventsInp {

	public static void main(String[] args) {
		String path2DemandFile = args[0];
		String path2InpFile = args[1];
		String path2NewInpFile = args[2];

		for (int i = 0; i < 31; i++) {
			HashMap<Id<Trip>, Integer> tripDemands = MatchTrips.readTripDemands(ConvEvents.insertVersNumInFilepath(path2DemandFile,i));
			ConvEvents2Inp convEvents = new ConvEvents2Inp();
			convEvents.writeRoutes(tripDemands, path2InpFile, ConvEvents.insertVersNumInFilepath(path2NewInpFile,i));
		}
	}

}
