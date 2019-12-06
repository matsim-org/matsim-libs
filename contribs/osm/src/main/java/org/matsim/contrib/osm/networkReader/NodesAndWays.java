package org.matsim.contrib.osm.networkReader;


import java.util.Map;

class NodesAndWays {
	private final Map<Long, ProcessedOsmNode> nodes;
	private final Map<Long, ProcessedOsmWay> ways;

	public NodesAndWays(Map<Long, ProcessedOsmNode> nodes, Map<Long, ProcessedOsmWay> ways) {
		this.nodes = nodes;
		this.ways = ways;
	}

	public Map<Long, ProcessedOsmNode> getNodes() {
		return nodes;
	}

	public Map<Long, ProcessedOsmWay> getWays() {
		return ways;
	}
}