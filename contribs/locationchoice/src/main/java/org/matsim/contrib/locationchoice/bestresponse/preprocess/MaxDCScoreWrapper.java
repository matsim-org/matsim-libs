package org.matsim.contrib.locationchoice.bestresponse.preprocess;

import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MaxDCScoreWrapper  {
	public static final String ELEMENT_NAME = "maxDcScoreWrapper";

	private ObjectAttributes personsMaxDCScoreUnscaled; 
	
	public ObjectAttributes getPersonsMaxDCScoreUnscaled() {
		return personsMaxDCScoreUnscaled;
	}

	public void setPersonsMaxDCScoreUnscaled(ObjectAttributes personsMaxDCScoreUnscaled) {
		this.personsMaxDCScoreUnscaled = personsMaxDCScoreUnscaled;
	}
	
}
