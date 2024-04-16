package org.matsim.contrib.common.zones;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.util.ZoneFinder;

import java.util.*;

public class ZoneSystemImpl implements ZoneSystem {

	private final Map<Id<Zone>, Zone> zones = new IdMap<>(Zone.class);
	private final IdMap<Link, Zone> link2zone = new IdMap<>(Link.class);

	private final IdMap<Node, Zone> nodeToZoneMap = new IdMap<>(Node.class);

	private final IdMap<Zone, List<Link>> zoneToLinksMap = new IdMap<>(Zone.class);

	public ZoneSystemImpl(Collection<Zone> zones) {
		zones.forEach(zone -> this.zones.put(zone.getId(), zone));
	}

	public ZoneSystemImpl(Collection<Zone> zones, ZoneFinder zoneFinder, Network network) {
		zones.forEach(zone -> this.zones.put(zone.getId(), zone));

		IdMap<Node, Zone> nodeToZoneMap = ZoneSystemUtils.createNodeToZoneMap(network, zoneFinder);
		IdMap<Link, Zone> linkToZoneMap = ZoneSystemUtils.createLinkToZoneMap(network, zoneFinder);
		this.nodeToZoneMap.putAll(nodeToZoneMap);
		this.link2zone.putAll(linkToZoneMap);

		for (Link link : network.getLinks().values()) {
			zoneFinder.findZone(link.getToNode().getCoord()).ifPresent(zone -> {
                List<Link> links = zoneToLinksMap.computeIfAbsent(zone.getId(), zoneId1 -> new ArrayList<>());
                links.add(link);
            });
		}
	}

	/**
	 * @param link
	 * @return the the {@code DrtZone} that contains the {@code linkId}. If the given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
	@Override
	public Optional<Zone> getZoneForLinkId(Id<Link> link) {
		return Optional.ofNullable(link2zone.get(link));
	}

	@Override
	public Optional<Zone> getZoneForNodeId(Id<Node> nodeId) {
		return Optional.ofNullable(nodeToZoneMap.get(nodeId));
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return zoneToLinksMap.getOrDefault(zone, Collections.emptyList());
	}

	/**
	 * @return the zones
	 */
	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}
}
