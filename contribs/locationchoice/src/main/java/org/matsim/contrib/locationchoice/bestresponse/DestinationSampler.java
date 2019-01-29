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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;

/**
 * Chooses a stable sample of facilities for every person 
 * 
 * At the moment it is only working for powers of 10 in the range [0..100]
 * The sample must remain stable over the course of iterations. Thus, person as well as facility k values
 * are used. 
 * 		
 * (Conditional) probability (in %) of a facility k-value matching the given person k-value is 
 * this.samplePercent	
 * example: this.samplePercent = 10;
 * person i has a personValue k between 0 and 9
 * probability that facilityValue is k is expected 0.1 (or 10%)
 * 
 */
public class DestinationSampler {
	
	private double[] facilitiesKValues;
	private double[] personsKValues;
	double samplePercent = 100.0;
	
	public DestinationSampler(double[] personsKValues, double[] facilitiesKValues, DestinationChoiceConfigGroup module) {
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
		this.samplePercent = module.getDestinationSamplePercent();
	}
	
	public boolean sample(int facilityIndex, int personIndex) { 
		
		if (Math.ceil(this.samplePercent) == 100) return true;		
				
		// assign the person randomly but frozen a number between 0 and 100/samplePercent:
		int personValue = (int)Math.floor(100.0 / samplePercent * this.personsKValues[personIndex]);
		
		// assign the facility randomly but frozen a number between 0 and 100/samplePercent:
		int facilityValue = (int)Math.floor(100.0 / samplePercent * this.facilitiesKValues[facilityIndex]);
		
		// return true if the facility has the same number as the person. This will happen for samplePercent of all facilities:
		return (facilityValue == personValue);		
	}
}