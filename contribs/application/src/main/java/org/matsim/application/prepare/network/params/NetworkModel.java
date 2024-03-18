package org.matsim.application.prepare.network.params;

/**
 * A model for estimating network parameters.
 */
public interface NetworkModel {

	/**
	 * Flow Capacity (per lane)
	 */
	default FeatureRegressor capacity(String junctionType) {
		return null;
	}

	/**
	 * Speed factor (relative to free flow speed).
	 */
	default FeatureRegressor speedFactor(String junctionType) {
		return null;
	}

}
