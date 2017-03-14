/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class BicycleTravelTime implements TravelTime {

	/**
	 * in this class traveltime is calculated depending on the following parameters:
	 * surface, slope/elevation
	 * 
	 * following parameters are supposed to be implemented
	 * cyclewaytype, smoothness? (vs surface), weather/wind?, #crossings (info in nodes)
	 * 
	 * 
	 * following parameters are supposed to be implemented to the disutility
	 * traveltime, distance, surface, smoothness, slope/elevation, #crossings (info in nodes), cyclewaytype, 
	 * size of street aside, weather/wind, parkende autos?
	 * 
	 */
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {

		double travelTime = link.getLength()/link.getFreespeed();
		return travelTime;	
	}
}