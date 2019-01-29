/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package org.matsim.contrib.av.intermodal.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public interface VariableAccessEgressTravelDisutility {

	/**
	 * 
	 * @param person
	 * @param coord
	 * @param toCoord
	 * @return
	 */
	Leg getAccessEgressModeAndTraveltime(Person person, Coord coord, Coord toCoord, double time);
	/**
	 * 
	 * @param mode
	 * @return whether a mode is teleported. 
	 * Non-teleported modes require an additional stage activity for the agent to get from street network to pt network
	 */
	boolean isTeleportedAccessEgressMode(String mode);
	
}
