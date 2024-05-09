package org.matsim.application.prepare.network.params.hbs;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;

/**
 * Road capacity for residential roads or other of similar or lower category.
 */
public class HBSSideRoadCapacity implements Predictor {
	@Override
	public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {

		return 0;


	}

	/**
	 * Capacity of a side road merging into a main road.
	 */
	public double capacityMerging(Object2DoubleMap<String> features) {

		// See HBS page 5-20, eq. S5-12 table S5-5

		// mean Folgezeitlücken of the different combinations
		double tf = 3.5;

		// mean Grenzzeitlücke
		double tg = 6.36;

		// traffic volume on the main road
		// here an assumption needs to be made, normally the capacity of the side roads would not be constant
		double qP = 400;

		return Math.exp( (-qP/3600) * (tg - tf/2)) * 3600/tf;
	}

}
