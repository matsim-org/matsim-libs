package org.matsim.contrib.common.zones.systems.geom_free_zones;

import com.google.common.base.Verify;
import jakarta.validation.constraints.Positive;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Generate geometry-free zones based on network directly. Additional attribute in link is required: zone_id
 */
public class GeometryFreeZoneSystemParams extends ZoneSystemParams {

	public static final String SET_NAME = "GeometryFreeZoneSystem";
	public static final String ZONE_ID = "zoneId";

	public GeometryFreeZoneSystemParams() {
		super(SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Network network = ScenarioUtils.createScenario(config).getNetwork();
		// Here, we check one arbitrary link from the (sub-)network used for DVRP/DRT vehicles and see if there is a zone ID attribute
		Verify.verify(network.getLinks().values().iterator().next().getAttributes().getAttribute(ZONE_ID) != null,
			"Zone id attribute not set");
	}
}
