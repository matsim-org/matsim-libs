package org.matsim.application.prepare.network.params;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;

/**
 * Predictor interface for regression.
 * @deprecated Use {@link Predictor} instead.
 */
@Deprecated
public interface FeatureRegressor extends Predictor {


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

	default double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
		return predict(features);
	}

	/**
	 * Predict values with adjusted model params.
	 */
	default double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories, double[] params) {
		return predict(features, params);
	}

	/**
	 * Return data that is used for internal prediction function (normalization already applied).
	 */
	default double[] getData(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
		return getData(features);
	}

}
