package org.matsim.contrib.shared_mobility.routing;

import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public class FreefloatingInteractionFinder implements InteractionFinder {
	private final Network network;

	public FreefloatingInteractionFinder(Network network) {
		this.network = network;
	}

	@Override
	public Optional<InteractionPoint> findPickup(Facility originFacility) {
		return findInteraction(originFacility);
	}

	@Override
	public Optional<InteractionPoint> findDropoff(Facility destinationFacility) {
		return findInteraction(destinationFacility);
	}

	private Optional<InteractionPoint> findInteraction(Facility facility) {
		Link link = FacilitiesUtils.decideOnLink(facility, network);

		if (link == null) {
			return Optional.empty();
		}

		return Optional.of(InteractionPoint.of(link));
	}
}
