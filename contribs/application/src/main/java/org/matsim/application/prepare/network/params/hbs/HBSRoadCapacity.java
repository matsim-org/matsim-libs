package org.matsim.application.prepare.network.params.hbs;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.matsim.application.prepare.network.params.FeatureRegressor;

/**
 * See {@link HBSNetworkParams}.
 */
class HBSRoadCapacity implements FeatureRegressor {
	@Override
	public double predict(Object2DoubleMap<String> ft) {
		return 0;
	}
}
