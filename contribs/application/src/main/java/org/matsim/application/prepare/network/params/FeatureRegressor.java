package org.matsim.application.prepare.network.params;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

/**
 * Predictor interface for regression.
 */
public interface FeatureRegressor {


	/**
	 * Predict value from given features.
	 */
	double predict(Object2DoubleMap<String> ft);

	/**
	 * Predict values with adjusted model params.
	 */
	default double predict(Object2DoubleMap<String> ft, double[] params) {
		throw new UnsupportedOperationException("Not implemented");
	}


	/**
	 * Return data that is used for internal prediction function (normalization already applied).
	 */
	default double[] getData(Object2DoubleMap<String> ft) {
		throw new UnsupportedOperationException("Not implemented");
	}

}
