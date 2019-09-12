package org.matsim.contrib.locationchoice.frozenepsilons;

import org.matsim.utils.objectattributes.ObjectAttributes;

class MaxDCScoreWrapper  {
	public static final String ELEMENT_NAME = "maxDcScoreWrapper";

	private ObjectAttributes personsMaxDCScoreUnscaled; 
	
	public ObjectAttributes getPersonsMaxDCScoreUnscaled() {
		return personsMaxDCScoreUnscaled;
	}

	public void setPersonsMaxDCScoreUnscaled(ObjectAttributes personsMaxDCScoreUnscaled) {
		this.personsMaxDCScoreUnscaled = personsMaxDCScoreUnscaled;
	}
	
}
