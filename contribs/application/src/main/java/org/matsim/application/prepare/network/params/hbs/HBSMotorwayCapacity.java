package org.matsim.application.prepare.network.params.hbs;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;

/**
 * Capacity for motorways.
 */
public class HBSMotorwayCapacity implements Predictor {
	@Override
	public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {
		return 0;
	}
}
