package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ZeroParkingCapacityInitializer implements ParkingCapacityInitializer {
	private final Network network;

	@Inject
	ZeroParkingCapacityInitializer(Network network) {
		this.network = network;
	}

	@Override
	public Map<Id<Link>, ParkingInitialCapacity> initialize() {
		Map<Id<Link>, ParkingInitialCapacity> res = new HashMap<>(network.getLinks().size());
		for (Link link : network.getLinks().values()) {
			int onStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_ON_STREET_SPOTS)).orElse(0);
			int offStreet = (int) Optional.ofNullable(link.getAttributes().getAttribute(LINK_OFF_STREET_SPOTS)).orElse(0);
			res.put(link.getId(), new ParkingInitialCapacity(onStreet + offStreet, 0));
		}
		return res;
	}
}
