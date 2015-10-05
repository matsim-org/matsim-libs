package org.matsim.contrib.cadyts.measurement;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * Cadyts measurements are attached to "facilities", such as links.  With distributions, the measurements are attached to nothing.
 * To satisfy Cadyts, Measurement is used as facility to which cadyts can attach the measurement.
 * 
 * @author nagel
 *
 */
public class Measurement implements Identifiable<Measurement>, Comparable<Measurement> {
	// might be able to just feed Id<Measurement> to Cadyts.  On the other hand, Measurement is a good place
	// to attach the lower bound to.

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

	@Override
	public int compareTo(Measurement o) {
		return this.id.compareTo( o.getId() ) ;
	}

}