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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author pboesch
 *
 */
public interface SpatialCuttingStrategy {
	
	/**
	 * Based on the implementations of the different spatial cutting strategies,
	 * this method returns if a trip is to be considered for processing (TRUE) or not (FALSE).
	 * 
	 * @param network	in which the trip took place
	 * @param startLink	of the trip
	 * @param endLink	of the trip
	 * @return	TRUE if the trip is to consider
	 */
	public boolean spatiallyConsideringTrip(Network network, Id<Link> startLink, Id<Link> endLink);

}
