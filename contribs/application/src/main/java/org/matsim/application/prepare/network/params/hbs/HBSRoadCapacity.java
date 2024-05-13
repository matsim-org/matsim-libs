package org.matsim.application.prepare.network.params.hbs;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;

/**
 * Capacity for general roads, that are not motorways or residential roads.
 */
public class HBSRoadCapacity implements Predictor {
	/**
	 * Capacity on primary roads (Landstraße)
	 */
	private static double capacityHighway(int lanes) {

		// There is currently no way to differentiate Landstraße and Stadtstraße which can both have up to 70km/h

		if (lanes == 1)
			return 2846.990116534646;

		if (lanes == 2)
			return 3913.3439999999996 / 2;

		// Own assumption of increasing capacity with more lanes
		// This is not covered by the HBS and is a very rare case
		return (3913.3439999999996 * 1.3) / lanes;
	}

	/**
	 * Capacity on a side road merging into a main road at a junction.
	 *
	 * @param qP the vehicle volume of the main road
	 */
	private static double capacityMerging(double qP) {

		// See HBS page 5-20, eq. S5-12 table S5-5

		// mean Folgezeitlücken of the different combinations
		double tf = 3.5;

		// mean Grenzzeitlücke
		double tg = 6.36;


		return Math.exp((-qP / 3600) * (tg - tf / 2)) * 3600 / tf;
	}

	/**
	 * Capacity of a side road of type merging into a main road.
	 *
	 * @param roadType type of the target road
	 * @param mainType type of the higher priority road
	 */
	private static double capacityMerging(String roadType, String mainType) {

		if (mainType.equals("primary"))
			return capacityMerging(600);

		return capacityMerging(400);
	}

	@Override
	public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {

		// Speed in km/h
		int speed = (int) Math.round(features.getDouble("speed") * 3.6);
		int lanes = (int) features.getOrDefault("lanes", 1);

		if (speed >= 70) {
			return capacityHighway(lanes);
		}

		String merging = categories.get("is_merging_into");

		// Only merging with a single lane road is considered
		if (!merging.isEmpty() && lanes == 1) {
			return capacityMerging(categories.get("highway_type"), merging);
		}

		// Capacity for city roads
		if (speed >= 40 || lanes >= 2) {
			return switch (lanes) {
				case 1 -> 1139.0625;
				case 2 -> 2263.438914027149 / 2;
				// Own assumption, rare edge-case
				default -> 2263.438914027149 * 1.3 / lanes;
			};
		}

		// Remaining are residential which are assumed to have high urbanisation
		return 800.0/lanes;
	}

}
