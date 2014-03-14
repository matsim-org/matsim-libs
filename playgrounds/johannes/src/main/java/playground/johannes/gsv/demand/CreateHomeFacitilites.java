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

/**
 * 
 */
package playground.johannes.gsv.demand;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * @author johannes
 *
 */
public class CreateHomeFacitilites {

	public static final double DEFAULT_FREESPEED_THRESHOLD = 70/3.6;
	
	public static final double DEFAULT_FACILITY_LENGTH = 50;
	
	private static double freespeedThreshold = DEFAULT_FREESPEED_THRESHOLD;
	
	private static double facilityLength = DEFAULT_FACILITY_LENGTH;
	
	public static void createFacilitites(Network network) {
		for(Link link : network.getLinks().values()) {
			if(link.getFreespeed() < freespeedThreshold) {
				
			}
		}
	}
}
