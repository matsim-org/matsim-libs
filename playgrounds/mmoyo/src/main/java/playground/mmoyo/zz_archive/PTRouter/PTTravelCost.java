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

package playground.mmoyo.zz_archive.PTRouter;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates the cost of links for the routing algorithm
 */
public class PTTravelCost implements TravelDisutility{
	private PTTravelTime ptTravelTime;
	
	public PTTravelCost(final PTTravelTime ptTravelTime) {
		this.ptTravelTime = ptTravelTime;
	}
	
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double cost = ptTravelTime.getLinkTravelTime(link, time, person, vehicle) ;  

		String type = ((LinkImpl)link).getType();
		if (type.equals( PTValues.DETTRANSFER_STR ) || type.equals( PTValues.TRANSFER_STR )){
			cost = (cost* PTValues.walkCoefficient)+ PTValues.transferPenalty;
			//cost = (cost + PTValues.transferPenalty) * PTValues.timeCoefficient;
			//cost = (cost * 	PTValues.timeCoefficient) + (link.getLength() * PTValues.distanceCoefficient);
		}else if (type.equals( PTValues.STANDARD_STR )){
			cost = (cost * 	PTValues.timeCoefficient) + (link.getLength() * PTValues.distanceCoefficient);
		}else if (type.equals( PTValues.ACCESS_STR ) || type.equals( PTValues.EGRESS_STR )){
			cost = cost * PTValues.walkCoefficient; 
		}else{
			throw new java.lang.RuntimeException("the pt link does not have a defined type: " + link.getId());
		}
		return cost;
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
}