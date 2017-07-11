package org.matsim.contrib.opdyts.example.networkparameters;

import org.matsim.api.core.v01.network.Link;

/**
 * Created by michaelzilske on 08/10/15.
 */
class LinkParameters {

	// -------------------- MEMBERS --------------------

	private final double freespeed;

	private final double flowCapacity;

	private final double nofLanes;

	// -------------------- CONSTRUCTION --------------------

	LinkParameters(double freespeed, double flowCapacity, double nofLanes) {
		this.freespeed = freespeed;
		this.flowCapacity = flowCapacity;
		this.nofLanes = nofLanes;
	}

	LinkParameters(final Link link) {
		this(link.getFreespeed(), link.getCapacity(), link.getNumberOfLanes());
	}

	// -------------------- GETTERS --------------------

	double getFreespeed() {
		return freespeed;
	}

	double getFlowCapacity() {
		return flowCapacity;
	}

	double getNofLanes() {
		return nofLanes;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		return "(speed=" + this.freespeed + ";cap=" + this.flowCapacity + ",lanes=" + this.nofLanes + ")";
	}

}
