package org.matsim.osmNetworkReader;

import de.topobyte.osm4j.core.model.iface.OsmWay;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Getter
class NodesAndWays {
	private final Map<Long, LightOsmNode> nodes;
	private final Set<OsmWay> ways;
}