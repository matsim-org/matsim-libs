package org.matsim.application.prepare.network.params;

import org.matsim.application.prepare.Predictor;

/**
 * A model for estimating network parameters.
 */
public interface NetworkModel {

	/**
	 * Flow Capacity (per lane).
	 */
	default Predictor capacity(String junctionType, String highwayType) {
		throw new UnsupportedOperationException("Capacity model not implemented for class: " + getClass().getName());
	}

	/**
	 * Speed factor (relative to allowed speed).
	 */
	default Predictor speedFactor(String junctionType, String highwayType) {
		throw new UnsupportedOperationException("Speed factor model not implemented for class: " + getClass().getName());
	}

}
