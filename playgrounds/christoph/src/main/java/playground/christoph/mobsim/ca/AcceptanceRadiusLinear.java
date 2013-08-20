/* *********************************************************************** *
 * project: org.matsim.*
 * AcceptanceRadiusLinear.java
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

public class AcceptanceRadiusLinear {

//properties
//    startAcceptanceRadius;
//    timeToDoubleStartRadius;
//end

	private final double startAcceptanceRadius;
	private final double timeToDoubleStartRadius;
	
//methods
//    function this = AcceptanceRadiusLinear(startRadius, timeToDoubleRadius)
//        this.startAcceptanceRadius = startRadius;
//        this.timeToDoubleStartRadius = timeToDoubleRadius;
//    end
	public AcceptanceRadiusLinear(double startAcceptanceRadius, double timeToDoubleStartRadius) {
		this.startAcceptanceRadius = startAcceptanceRadius;
		this.timeToDoubleStartRadius = timeToDoubleStartRadius;
	}
	
//    function r = acceptanceRadius(this, elapsedSearchTime)
//        r = this.startAcceptanceRadius + (elapsedSearchTime).* this.startAcceptanceRadius./(this.timeToDoubleStartRadius);
//    end
	public double getAcceptanceRadius(double elapsedSearchTime) {
		return this.startAcceptanceRadius + elapsedSearchTime * this.startAcceptanceRadius / this.timeToDoubleStartRadius;
	}

}
