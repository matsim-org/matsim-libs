/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.mapping;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author polettif
 */
public class PTMapping {
	
	public static void main(final String[] args) {
		
	}

	/**
	 * Maps the basic schedule (with transitStops, routeProfile and departures but without route) to a network.
	 *
	 * @param schedule MATSim Transit Schedule without route (sequence of links)
	 * @param network street network
	 *
	 * Based on boescpa.PTMapping
	 */
	public static void MTSmapping(TransitSchedule schedule, Network network) {

	}

}