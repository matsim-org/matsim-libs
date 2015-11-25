/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.boescpa.analysis.trips.tripCreation.spatialCuttings;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

/**
 * Spatial cutting strategy for trip processing.
 * 
 * NoCutting returns TRUE for all trips and thus does not cut any trip.
 * 
 * @author pboesch
 */
public class NoCutting implements SpatialCuttingStrategy {

	public NoCutting() {}

	@Override
	public boolean spatiallyConsideringTrip(Network network, Id startLink, Id endLink) {
		return true;
	}

}
