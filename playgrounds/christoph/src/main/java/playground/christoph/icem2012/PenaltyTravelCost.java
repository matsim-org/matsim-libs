/* *********************************************************************** *
 * project: org.matsim.*
 * PenaltyTravelCost.java
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

package playground.christoph.icem2012;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;

public class PenaltyTravelCost implements TravelDisutility {

	private final TravelDisutility travelCost;
	private final CoordAnalyzer coordAnalyzer;
	
	public PenaltyTravelCost(TravelDisutility travelCost, CoordAnalyzer coordAnalyzer) {
		this.travelCost = travelCost;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double tc = travelCost.getLinkTravelDisutility(link, time, person, vehicle);
		double penaltyFactor = 1.0;
		
		if (time > EvacuationConfig.evacuationTime) {			
			if (coordAnalyzer.isLinkAffected(link)) {
				// penalty factor of 3 per hour
				double dt = time - EvacuationConfig.evacuationTime;
				penaltyFactor = 1.0 + (3 * dt/3600.0);
			}
		}
		
		return tc * penaltyFactor;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}