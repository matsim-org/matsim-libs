package org.matsim.contrib.common.zones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import javax.annotation.Nullable;
import java.util.Map;

public interface ZoneSystem {
	@Nullable
	Zone getZoneForLinkId(Id<Link> linkId);

	Map<Id<Zone>, Zone> getZones();
}
