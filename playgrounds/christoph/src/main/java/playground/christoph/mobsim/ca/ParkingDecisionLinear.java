/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingDecisionLinear.java
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

public class ParkingDecisionLinear extends ParkingDecision {

//	 function this = ParkingDecisionLinear(spatialResolution, startAcceptanceRadius, decreasingSlope, timeToDoubleRadius)
//	            this = this@ParkingDecision(spatialResolution);
//	            this.acceptanceRadiusModel = AcceptanceRadiusLinear(startAcceptanceRadius,timeToDoubleRadius); %AcceptanceRadiusQuadratic(startAcceptanceRadius, timeToDoubleRadius);
//	            this.decreasingSlope = decreasingSlope;
//	        end
	private final double decreasingSlope;
	private final AcceptanceRadiusLinear acceptanceRadiusLinear;
	
	public ParkingDecisionLinear(double spatialResoluation, double startSearchRadius,
			double startAcceptanceRadius, double decreasingSlope, double timeToDoubleRadius) {
		super(spatialResoluation, startSearchRadius);
		this.decreasingSlope = decreasingSlope;
		this.acceptanceRadiusLinear = new AcceptanceRadiusLinear(startAcceptanceRadius, timeToDoubleRadius);
	}

//    function p = parkProbability(this, elapsedSearchTime, distanceToDestination)
//    
//     p = 1-this.decreasingSlope.*(distanceToDestination - this.acceptanceRadiusModel.acceptanceRadius(elapsedSearchTime));
//%                 if(p > 1)
//%                     p = 1;
//%                 elseif(p < 0)
//%                     p = 0;
//%                 end       
//     tooBig = (p>1); % set values to one, where value too big
//     tooSmall = (p<0); % set Values to zero, where value too small
//     p = p - p.*tooBig + tooBig - p.*tooSmall; %Bsp [ -3 0.5 3] tooBig = [0 0 1], tooSmall = [1 0 0] -> p = [0 0.5 1] 
//     
// end
	@Override
	// TODO: is the distance a single value or a list/array?
	public double parkProbability(double elapsedSearchTime, double distanceToDestination) {

		double p = 1 - this.decreasingSlope * (distanceToDestination - this.acceptanceRadiusLinear.getAcceptanceRadius(elapsedSearchTime));
		
		// TODO: what is going on here?
		
		return p;
	}

}
