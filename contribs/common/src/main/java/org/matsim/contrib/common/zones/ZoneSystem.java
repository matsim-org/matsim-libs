package org.matsim.contrib.common.zones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ZoneSystem {
	Optional<Zone> getZoneForLinkId(Id<Link> link);

	Optional<Zone> getZoneForNodeId(Id<Node> nodeId);

	List<Link> getLinksForZoneId(Id<Zone> zone);

	Map<Id<Zone>, Zone> getZones();
}
