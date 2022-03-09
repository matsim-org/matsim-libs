package org.matsim.contrib.shared_mobility.routing;

import java.util.Optional;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.shared_mobility.io.SharingServiceSpecification;
import org.matsim.contrib.shared_mobility.io.SharingStationSpecification;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public class StationBasedInteractionFinder implements InteractionFinder {
	private final Network network;
	private final QuadTree<SharingStationSpecification> stations;
	private final double maxmimumDistance;

	public StationBasedInteractionFinder(Network network, SharingServiceSpecification specification,
			double maximumDistance) {
		this.network = network;
		this.maxmimumDistance = maximumDistance;

		this.stations = QuadTrees.createQuadTree(specification.getStations(),
				spec -> network.getLinks().get(spec.getLinkId()).getCoord(), 1000.0);
	}

	@Override
	public Optional<InteractionPoint> findPickup(Facility originFacility) {
		return findStation(originFacility);
	}

	@Override
	public Optional<InteractionPoint> findDropoff(Facility destinationFacility) {
		return findStation(destinationFacility);
	}

	private Optional<InteractionPoint> findStation(Facility nearbyFacility) {
		Link nearbyLink = FacilitiesUtils.decideOnLink(nearbyFacility, network);

		if (nearbyLink != null) {
			SharingStationSpecification station = stations.getClosest(nearbyLink.getCoord().getX(),
					nearbyLink.getCoord().getY());
			Link stationLink = network.getLinks().get(station.getLinkId());

			if (CoordUtils.calcEuclideanDistance(stationLink.getCoord(), nearbyLink.getCoord()) <= maxmimumDistance) {
				return Optional.of(InteractionPoint.of(station));
			}
		}

		return Optional.empty();
	}
}
