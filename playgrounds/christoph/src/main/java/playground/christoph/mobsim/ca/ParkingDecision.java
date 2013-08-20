/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingDecision.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.ca;

public abstract class ParkingDecision {

//properties
//    decreasingSlope; % defines the point where
//    acceptanceRadiusModel;  
//    spatialResolution;
//    startSearchRadius = 0;
//end
	
	private final double spatialResoluation;
	private final double startSearchRadius;
//
//methods
//    function this = ParkingDecision(spatialResolution)
//        this.spatialResolution = spatialResolution;
//    end
	public ParkingDecision(double spatialResoluation, double startSearchRadius) {
		this.spatialResoluation = spatialResoluation;
		this.startSearchRadius = startSearchRadius;
	}
    
//    function p = isSearchStarting(this, distanceToDestination)
//         p = (distanceToDestination <= this.startSearchRadius || distanceToDestination < 2*this.spatialResolution);  
//    end       
	public boolean isSearchStarting(double distanceToDestination) {
		return distanceToDestination <= this.startSearchRadius || distanceToDestination < 2* this.spatialResoluation;
	}
//
//methods(Abstract)
//  p = parkProbability(this, elapsedSearchTime, distanceToDestination)
//end
	public abstract double parkProbability(double elapsedSearchTime, double distanceToDestination);
}
