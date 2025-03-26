package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ZeroParkingCapacityInitializer implements ParkingCapacityInitializer {
	private final Network network;
	private final Config config;

	@Inject
	ZeroParkingCapacityInitializer(Network network, Config config) {
		this.network = network;
		this.config = config;
	}

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize() {
		Map<Id<Link>, ParkingInitialCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(ParkingUtils.LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(ParkingUtils.LINK_OFF_STREET_SPOTS)).orElse(0);

			int newCapacity = (int) Math.ceil((onStreet + offStreet) * config.qsim().getStorageCapFactor());
			res.put(link.getId(), new ParkingInitialCapacity(newCapacity, 0));
		}
		return res;
	}
}
