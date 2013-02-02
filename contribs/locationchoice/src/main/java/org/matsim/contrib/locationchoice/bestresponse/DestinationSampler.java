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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class DestinationSampler {
	
	private ObjectAttributes facilitiesKValues;
	private ObjectAttributes personsKValues;
	double samplePercent = 100.0;
	
	public DestinationSampler(ObjectAttributes personsKValues, ObjectAttributes facilitiesKValues,  
			LocationChoiceConfigGroup config) {
		this.facilitiesKValues = facilitiesKValues;
		this.personsKValues = personsKValues;
		this.samplePercent = Double.parseDouble(config.getDestinationSamplePercent());
	}
	
	private static int wrnCnt = 0 ;
	
	// at the moment only working for powers of 10
	public boolean sample(Id facilityId, Id personId) { 
		if (Math.ceil(this.samplePercent) == 100) return true;
		
		if (wrnCnt < 1 ) {
			wrnCnt++ ;
			Logger.getLogger(this.getClass()).warn("I do not understand what is happening when samplePercent!=100.") ;
			Logger.getLogger(this.getClass()).warn("E.g. samplePercent=10.  Then (int)(100./10.*rnd) = an integer number between 0 and 9.") ;
			Logger.getLogger(this.getClass()).warn("That was for facilities; same will happen for persons.") ;
			Logger.getLogger(this.getClass()).warn("Proba that they are equal is 1% (=0.1*0.1), and not 10% as it seems to pretend.") ;
			Logger.getLogger(this.getClass()).warn("???? kai, jan'13") ;
		}
		/*
		 * ah: Not sure if I should even try to think today, but let me see ...
		 * Isn't it a cond. proba?
		 * personValue = 1 -> 10% proba for facilityValue == 1
		 * personValue = 2 -> 10% proba for facilityValue == 2
		 * i.e., whatever personValue might is, we have 10% proba for facilityValue being the same.
		 * In other words, we have 100 combinations and not only 1 but 10 matchings. 
		 * Or do I make the same error in thinking again?
		 * 
		 * This is the case for samplePercent == 10, need to think about the other percentages. 
		 * Maybe we have another problem there.
		 */
		
		int facilityValue = (int)Math.floor(100.0 / samplePercent * 
				(Double) this.facilitiesKValues.getAttribute(facilityId.toString(), "k"));
		int personValue = (int)Math.floor(100.0 / samplePercent * 
				(Double) this.personsKValues.getAttribute(personId.toString(), "k"));
		return (facilityValue == personValue);
		
	}
}
