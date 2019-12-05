package org.matsim.osmNetworkReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Coord;

import java.util.List;

@RequiredArgsConstructor
@Getter
class ProcessedOsmNode {

	private final long id;
	private final List<ProcessedOsmWay> filteredReferencedWays;
	private final Coord coord;

	boolean isWayReferenced(long wayId) {
		return filteredReferencedWays.stream()
				.anyMatch(way -> way.getId() == wayId);
	}

	boolean isIntersection() {
		return filteredReferencedWays.size() > 1;
	}
}