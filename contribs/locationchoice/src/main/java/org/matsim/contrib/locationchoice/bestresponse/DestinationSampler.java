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
	
	// at the moment only working for powers of 10
	public boolean sample(Id facilityId, Id personId) { 
		if (Math.ceil(this.samplePercent) == 100) return true;
		
		int facilityValue = (int)Math.floor(100.0 / samplePercent * 
				(Double) this.facilitiesKValues.getAttribute(facilityId.toString(), "k"));
		int personValue = (int)Math.floor(100.0 / samplePercent * 
				(Double) this.personsKValues.getAttribute(personId.toString(), "k"));
		return (facilityValue == personValue);
	}
}
