package org.matsim.application.prepare.network.params.hbs;

import org.matsim.application.prepare.Predictor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Capacity params calculated according to
 * "Handbuch für die Bemessung von Straßenverkehrsanlagen“ (HBS)
 */
public class HBSNetworkParams implements NetworkModel {

	private static final Predictor MOTORWAY = new HBSMotorwayCapacity();
	private static final Predictor ROAD = new HBSRoadCapacity();

	@Override
	public Predictor capacity(String junctionType, String highwayType) {

		// Traffic lights are not considered
		if (junctionType.equals("traffic_light")) {
			return null;
		}

		if (highwayType.startsWith("motorway")) {
			return MOTORWAY;
		}

		// All lower category roads
		return ROAD;
	}

}
