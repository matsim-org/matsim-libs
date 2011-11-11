package org.matsim.locationchoice.bestresponse;

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
