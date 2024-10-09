package org.matsim.contrib.common.zones.systems.geom_free_zones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;

import java.util.*;

import static org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystemParams.SET_NAME;
import static org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystemParams.ZONE_ID;

public class GeometryFreeZoneSystem implements ZoneSystem {
	private final Map<Id<Zone>, Zone> zones = new HashMap<>();
	private final Map<Id<Zone>, List<Link>> linksForZones = new HashMap<>();
	private final Map<Id<Link>, Zone> zoneForLinks = new HashMap<>();
	private final Map<Id<Node>, Zone> zoneForNodes = new HashMap<>();

	public GeometryFreeZoneSystem(Network network) {
		for (Link link : network.getLinks().values()) {
			String zoneIdString = (String) link.getAttributes().getAttribute(ZONE_ID);
			Id<Zone> zoneId = Id.create(zoneIdString, Zone.class);
			Zone zone;
			if (!zones.containsKey(zoneId)) {
				// zone has not yet been created
				zone = new ZoneImpl(zoneId, null, SET_NAME);
				zones.put(zoneId, zone);
				linksForZones.put(zoneId, List.of(link));
			} else {
				// zone already created
				zone = zones.get(zoneId);
				linksForZones.get(zoneId).add(link);
			}
			zoneForLinks.put(link.getId(), zone);
			zoneForNodes.put(link.getToNode().getId(), zone);
		}
	}

	@Override
	public Optional<Zone> getZoneForLinkId(Id<Link> link) {
		return zoneForLinks.containsKey(link) ? Optional.of(zoneForNodes.get(link)) : Optional.empty();
	}

	@Override
	public Optional<Zone> getZoneForNodeId(Id<Node> nodeId) {
		return zoneForNodes.containsKey(nodeId) ? Optional.of(zoneForNodes.get(nodeId)) : Optional.empty();
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return linksForZones.get(zone);
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return zones;
	}
}
