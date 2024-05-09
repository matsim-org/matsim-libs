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
	private static final Predictor SIDEROAD = new HBSSideRoadCapacity();

	@Override
	public Predictor capacity(String junctionType, String highwayType) {

		// Traffic lights are not considered
		if (junctionType.equals("traffic_light")) {
			return null;
		}

		if (highwayType.startsWith("motorway")) {
			return MOTORWAY;
		} else if (highwayType.startsWith("trunk") || highwayType.startsWith("primary") || highwayType.startsWith("secondary")) {
			return ROAD;
		}

		// All lower category roads
		return SIDEROAD;
	}

}
