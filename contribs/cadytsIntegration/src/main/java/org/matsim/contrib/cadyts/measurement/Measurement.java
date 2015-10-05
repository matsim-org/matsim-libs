package org.matsim.contrib.cadyts.measurement;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

public class Measurement implements Identifiable<Measurement> {

	private Id<Measurement> id;
	private double lowerBound;
	
	public Measurement( Id<Measurement> id, double lowerBound ) {
		this.id = id ;
		this.lowerBound = lowerBound ;
	}

	@Override
	public Id<Measurement> getId() {
		return this.id ;
	}

	public final double getLowerBound() {
		return this.lowerBound;
	}

}