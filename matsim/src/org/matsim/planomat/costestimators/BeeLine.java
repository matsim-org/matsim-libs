/* *********************************************************************** *
 * project: org.matsim.*
 * BeeLine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;

/**
 * Example implementation of a leg travel time estimation (the estimation of
 * a travel time between two activities at a given departure time).
 * 
 * It computes the travel time for the bee line distance between the from nodes
 * of the origin and destination links, at a speed of 20 km/h.
 * 
 * @author meisterk
 *
 */
public class BeeLine implements LegTravelTimeEstimator {
	
	public double getLegTravelTimeEstimation(
			IdI personId, 
			double departureTime, 
			Location origin, 
			Location destination,
			Route route,
			String mode) {

		
		Node originFromNode = ((Link)origin).getFromNode();
		Node destinationFromNode = ((Link)destination).getFromNode();
		
		double s = 
			originFromNode.getCoord().calcDistance(destinationFromNode.getCoord());
		
		double v = 20 / 3.6;	// km/h -> m/s
				
		double t = s/v;
		
		return t;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return new String("BeeLine implementation of LegTravelTimeEstimator");
	}

}
