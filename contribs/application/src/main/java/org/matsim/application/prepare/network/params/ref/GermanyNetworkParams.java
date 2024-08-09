package org.matsim.application.prepare.network.params.ref;

import org.matsim.application.prepare.network.params.FeatureRegressor;
import org.matsim.application.prepare.network.params.NetworkModel;

/**
 * Model trained on three region types in germany (metropole, city, rural).
 * It should work especially well on urban areas. For use on rural areas, additional fine-tuning should be considered.
 */
public final class GermanyNetworkParams implements NetworkModel {
	@Override
	public FeatureRegressor capacity(String junctionType, String highwayType) {
		return switch (junctionType) {
			case "traffic_light" -> GermanyNetworkParams_capacity_traffic_light.INSTANCE;
			case "right_before_left" -> GermanyNetworkParams_capacity_right_before_left.INSTANCE;
			case "priority" -> GermanyNetworkParams_capacity_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}

	@Override
	public FeatureRegressor speedFactor(String junctionType, String highwayType) {
		return switch (junctionType) {
			case "traffic_light" -> GermanyNetworkParams_speedRelative_traffic_light.INSTANCE;
			case "right_before_left" -> GermanyNetworkParams_speedRelative_right_before_left.INSTANCE;
			case "priority" -> GermanyNetworkParams_speedRelative_priority.INSTANCE;
			default -> throw new IllegalArgumentException("Unknown type: " + junctionType);
		};
	}
}
