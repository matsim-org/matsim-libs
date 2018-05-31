/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.minibus.schedule;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.PConfigGroup.StopLocationSelector;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Creates the transit stops valid for paratransit. Currently, only a replacement for a configurable version.
 * 
 * @author aneumann
 *
 */
public final class PStopsFactory {

	public static TransitSchedule createPStops(Network network, PConfigGroup pConfig, TransitSchedule transitSchedule){
		// return CreateStopsForAllCarLinks.createStopsForAllCarLinks(network, pConfig, transitSchedule);
		if (pConfig.getStopLocationSelector().equals(StopLocationSelector.allCarLinks)) {
			return CreatePStops.createPStops(network, pConfig, transitSchedule);
		} else if (pConfig.getStopLocationSelector().equals(StopLocationSelector.outsideJunctionAreas)) {
			return CreatePStopsOutsideJunctionAreas.createPStops(network, pConfig, transitSchedule);
		} else {
			throw new RuntimeException("unknown StopLocationSelector");
		}
	}
	
}
