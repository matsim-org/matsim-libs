package org.matsim.contrib.common.zones;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ZoneSystemImpl implements ZoneSystem {

	private final Map<Id<Zone>, Zone> zones = new IdMap<>(Zone.class);
	private final IdMap<Link, Zone> link2zone = new IdMap<>(Link.class);

	public ZoneSystemImpl(Collection<Zone> zones) {
		zones.forEach(zone -> this.zones.put(zone.getId(), zone));
		zones.stream()
			.flatMap(zone -> zone.getLinks().stream().map(link -> Pair.of(link.getId(), zone)))
			.forEach(idZonePair -> link2zone.put(idZonePair.getKey(), idZonePair.getValue()));
	}

	/**
	 * @param linkId
	 * @return the the {@code DrtZone} that contains the {@code linkId}. If the given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
	@Override
	@Nullable
	public Zone getZoneForLinkId(Id<Link> linkId) {
		return link2zone.get(linkId);
	}

	/**
	 * @return the zones
	 */
	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}
}
