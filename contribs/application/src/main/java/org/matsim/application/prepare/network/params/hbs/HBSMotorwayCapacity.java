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

		// speed in km/h
		int speed = (int) Math.round(features.getDouble("speed") * 3.6);
		int lanes = (int) features.getOrDefault("num_lanes", 1);

		// Capacity for 1 lane motorways is not defined in HBS
		double capacity = 2000;
		if (lanes == 2) {
			if (speed >= 130)
				capacity = 3700.0;
			else if (speed >= 120)
				capacity = 3800;
			else
				capacity = 3750;
		} else if (lanes == 3) {
			if (speed >= 130)
				capacity = 5300;
			else if (speed >= 120)
				capacity = 5400;
			else
				capacity = 5350;
		} else if (lanes >= 4) {
			if (speed >= 130)
				capacity = 7300;
			else
				capacity = 7400;
		}

		// Return capacity per lane
		return capacity / lanes;
	}
}
