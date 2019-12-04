package org.matsim.osmNetworkReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Coord;

import java.util.List;

@RequiredArgsConstructor
@Getter
class LightOsmNode {

	private final long id;
	private final List<ParallelWaysPbfParser.OsmWayWrapper> filteredReferencedWays;
	private final Coord coord;
}