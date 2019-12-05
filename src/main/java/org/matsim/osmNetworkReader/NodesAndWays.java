package org.matsim.osmNetworkReader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@Getter
class NodesAndWays {
	private final Map<Long, ProcessedOsmNode> nodes;
	private final Map<Long, ProcessedOsmWay> ways;
}