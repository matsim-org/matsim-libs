package org.matsim.contrib.osm.networkReader;

import org.matsim.api.core.v01.Coord;

import java.util.List;

class ProcessedOsmNode {

	private final long id;
	private final List<ProcessedOsmWay> filteredReferencedWays;
	private final Coord coord;

	public ProcessedOsmNode(long id, List<ProcessedOsmWay> filteredReferencedWays, Coord coord) {
		this.id = id;
		this.filteredReferencedWays = filteredReferencedWays;
		this.coord = coord;
	}

	public long getId() {
		return id;
	}

	public List<ProcessedOsmWay> getFilteredReferencedWays() {
		return filteredReferencedWays;
	}

	public Coord getCoord() {
		return coord;
	}

	boolean isWayReferenced(long wayId) {
		return filteredReferencedWays.stream()
				.anyMatch(way -> way.getId() == wayId);
	}

	boolean isIntersection() {
		return filteredReferencedWays.size() > 1;
	}
}