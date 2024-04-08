package org.matsim.application.prepare.network.params.hbs;

import org.matsim.application.prepare.network.params.FeatureRegressor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Capacity params calculated according to
 * "Handbuch für die Bemessung von Straßenverkehrsanlagen“ (HBS)
 */
public class HBSNetworkParams implements NetworkModel {

	@Override
	public FeatureRegressor capacity(String junctionType) {
		// TODO: should depend on junction type and or road type
		return new HBSRoadCapacity();
	}

	@Override
	public FeatureRegressor speedFactor(String junctionType) {
		throw new UnsupportedOperationException("Not implemented");
	}
}
