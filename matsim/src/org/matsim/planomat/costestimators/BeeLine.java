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

import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.network.Node;

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
	
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	final static double SPEED = 20.0 / 3.6; // 20km/h --> m/s
	
	@Override
	public String toString() {
		return "BeeLine implementation of LegTravelTimeEstimator";
	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			Act actOrigin, Act actDestination, Leg legIntermediate) {
		Node originFromNode = actOrigin.getLink().getFromNode();
		Node destinationFromNode = actDestination.getLink().getFromNode();
		/* why (unsafely) casting to Link to get a node? Why not just using
		 * Location.getCenter() as coordinate? In that case, that would be:
		 *   double d = origin.getCenter().calcDistance(destination.getCenter());
		 * After all, it's just a bee-line estimation, and if that's from a node
		 * or from another point nearby....    -marcel/12mar2008
		 */
		double distance = originFromNode.getCoord().calcDistance(destinationFromNode.getCoord());
		
		return distance / SPEED;
	}

}
