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

package playground.agarwalamit.mixedTraffic.patnaIndia.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author amit
 */

final class BikeTimeDistanceTravelDisUtility implements TravelDisutility  {

	private final TravelTime timeCalculator;
	private final double marginalCostOfTime;
	private final double marginalCostOfDistance;
	
	BikeTimeDistanceTravelDisUtility(
			final TravelTime timeCalculator,
			final double marginalCostOfTime_s,
			final double marginalCostOfDistance_m ){
		this.timeCalculator = timeCalculator;
		this.marginalCostOfTime = marginalCostOfTime_s;
		this.marginalCostOfDistance = marginalCostOfDistance_m;
		
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		return this.marginalCostOfTime * travelTime +  this.marginalCostOfDistance * link.getLength();
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime + this.marginalCostOfDistance * link.getLength();
	}
}
