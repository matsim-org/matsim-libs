package org.matsim.application.prepare.network.params.hbs;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import org.matsim.application.prepare.Predictor;

/**
 * Capacity for general roads, that are not motorways or residential roads.
 */
public class HBSRoadCapacity implements Predictor {
	/**
	 * Capacity on "Landstraße", often osm secondary. These numbers are taken from the HBS (see contribs/application/src/main/python/capacity/hbs.py)
	 */
	private static double capacityLandStr(int lanes, int curvature) {


		if (lanes == 1) {
			if (curvature == 1) return 1369.532383465354;

			if (curvature == 2) return 1117.1498589355958;

			if (curvature == 3) return 1048.5840399296935;

			if (curvature == 4) return 956.0314100959505;
		}

		if (lanes == 2) return 1956.6719999999998;


		// Own assumption of increasing capacity with more lanes
		// This is not covered by the HBS and is a very rare case
		return (1956.6719999999998 * 1.3) / lanes;
	}

	/**
	 * Bundesstraße with at least 70km/h, often osm primary or trunk
	 */
	private static double capacityBundesStr(int lanes) {

		if (lanes == 1)
			return 2033.868926820213;

		if (lanes == 2)
			return 3902.4390243902435 / 2;

		return (3902.4390243902435 * 1.3) / lanes;
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

	private static int curvatureCategory(double length, double curvature) {

		// for too short segment, curvature is not relevant
		if (length < 50 || curvature == 0)
			return 1;

		double sumChanges = curvature * (length / 1000);

		// Scale length of segment to at least 300m, because the manual recommends at least a certain segment length
		double ku = sumChanges / Math.max(0.3, length / 1000);

		if (ku > 150)
			return 4;
		if (ku > 100)
			return 3;
		if (ku > 50)
			return 2;
		return 1;

	}

	/**
	 * Capacity of a side road of type merging into a main road.
	 *
	 * @param roadType type of the target road
	 * @param mainType type of the higher priority road
	 */
	private static double capacityMerging(String roadType, String mainType) {

		if (mainType.equals("trunk") || mainType.equals("primary") || roadType.contains("residential"))
			// ~600 veh/h
			return capacityMerging(600);

		// ~800 veh/h
		return capacityMerging(400);
	}

	@Override
	public double predict(Object2DoubleMap<String> features, Object2ObjectMap<String, String> categories) {

		// Speed in km/h
		int speed = (int) Math.round(features.getDouble("speed") * 3.6);
		int lanes = (int) features.getOrDefault("num_lanes", 1);
		int curvature = curvatureCategory(features.getDouble("length"), features.getOrDefault("curvature", 0));
		String type = categories.get("highway_type");

		// Primary and trunk roads are often Bundesstraßen,
		// but only if they have a speed limit of 70 or more, this calculation is valid
		// From OSM alone it is not possible to distinguish anbaufreie Hauptverkehrsstraßen and Landstraße clearly
		if (speed >= 90 || ((type.contains("primary") || type.contains("trunk")) && speed >= 70)) {
			return capacityBundesStr(lanes);
		} else if (speed >= 70) {
			return capacityLandStr(lanes, curvature);
		}

		String merging = categories.get("is_merging_into");

		// Only merging with a single lane road is considered
		if (!merging.isEmpty() && lanes == 1) {
			return capacityMerging(type, merging);
		}

		// Capacity for city roads
		if (speed >= 40 || lanes >= 2 || features.getDouble("is_secondary_or_higher") == 1) {
			return switch (lanes) {
				case 1 -> 1139.0625;
				case 2 -> 2263.438914027149 / 2;
				// Own assumption, rare edge-case
				default -> 2263.438914027149 * 1.3 / lanes;
			};
		}

		// Remaining are residential which are assumed to have high urbanisation
		return 800.0 / lanes;
	}

}
