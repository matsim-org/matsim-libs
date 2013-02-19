package org.matsim.contrib.locationchoice.bestresponse.preprocess;

import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.api.internal.MatsimToplevelContainer;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class MaxDCScoreWrapper implements MatsimToplevelContainer {

	private ObjectAttributes personsMaxDCScoreUnscaled; 
	
	public ObjectAttributes getPersonsMaxDCScoreUnscaled() {
		return personsMaxDCScoreUnscaled;
	}

	public void setPersonsMaxDCScoreUnscaled(ObjectAttributes personsMaxDCScoreUnscaled) {
		this.personsMaxDCScoreUnscaled = personsMaxDCScoreUnscaled;
	}
	
	@Override
	public MatsimFactory getFactory() {
		return null;
	}
}
