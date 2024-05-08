package org.matsim.application.prepare.network.params.hbs;

import org.matsim.application.prepare.network.params.FeatureRegressor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Capacity params calculated according to
 * "Handbuch für die Bemessung von Straßenverkehrsanlagen“ (HBS)
 */
public class HBSNetworkParams implements NetworkModel {

	private static final FeatureRegressor MOTORWAY = new HSBMotorwayCapacity();
	private static final FeatureRegressor ROAD = new HBSRoadCapacity();

	@Override
	public FeatureRegressor capacity(String junctionType, String highwayType) {

		if (highwayType.startsWith("motorway")) {
			return MOTORWAY;
		} else if (junctionType.startsWith("priority")) {
			// All other roads
			return ROAD;
		}

		throw new UnsupportedOperationException("Unknown type: " + junctionType);
	}

}
