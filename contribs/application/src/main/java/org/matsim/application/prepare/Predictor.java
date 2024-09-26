package org.matsim.application.prepare;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * Predictor interface for arbitrary numeric values.
 */
public interface Predictor {


	/**
	 * Predict value from given features.
	 * @return predicted value, maybe NaN if no prediction is possible.
	 */
	double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories);

	/**
	 * Predict values with adjusted model params.
	 */
	default double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {
		throw new UnsupportedOperationException("Not implemented");
	}


	/**
	 * Return data that is used for internal prediction function (normalization already applied).
	 */
	default double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
		throw new UnsupportedOperationException("Not implemented");
	}

}
