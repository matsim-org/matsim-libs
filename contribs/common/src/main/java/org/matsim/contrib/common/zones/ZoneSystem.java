package org.matsim.contrib.common.zones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public interface ZoneSystem {
	@Nullable
	Zone getZoneForLink(Id<Link> link);

	Zone getZoneForNode(Node node);

	List<Link> getLinksForZone(Id<Zone> zone);


	Map<Id<Zone>, Zone> getZones();
}
