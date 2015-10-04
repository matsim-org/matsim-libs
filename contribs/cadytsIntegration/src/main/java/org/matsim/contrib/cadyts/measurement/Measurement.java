package org.matsim.contrib.cadyts.measurement;

import org.matsim.api.core.v01.Id;

public class Measurement {

	private Id<Measurement> id;
	
	public Measurement( Id<Measurement> id ) {
		this.id = id ;
	}

	public Id<Measurement> getMeasurementId() {
		return this.id ;
	}

}