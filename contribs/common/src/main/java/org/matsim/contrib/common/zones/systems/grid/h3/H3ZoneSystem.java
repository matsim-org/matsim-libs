package org.matsim.contrib.common.zones.systems.grid.h3;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.GridZoneSystem;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author nkuehnel / MOIA
 */
public class H3ZoneSystem implements GridZoneSystem {

	private final IdMap<Zone, Zone> zones = new IdMap<>(Zone.class);
	private final IdMap<Zone, List<Link>> zoneToLinksMap = new IdMap<>(Zone.class);

	private final TObjectLongMap<Coord> coordH3Cache = new TObjectLongHashMap<>();

	private final CoordinateTransformation toLatLong;
	private final CoordinateTransformation fromLatLong;

	private final int resolution;
	private final Network network;
	private final Predicate<Zone> filter;

	public H3ZoneSystem(String crs, int resolution, Network network, Predicate<Zone> filter) {
        this.fromLatLong = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, crs);
        this.toLatLong = TransformationFactory.getCoordinateTransformation(crs, TransformationFactory.WGS84);
        this.resolution = resolution;
		this.network = network;
		this.filter = filter;
		this.network.getLinks().values().forEach(l -> getZoneForCoord(l.getToNode().getCoord()));
    }


    @Override
	public Optional<Zone> getZoneForCoord(Coord coord) {

		long h3Address = getH3Cell(coord);
		Id<Zone> zoneId = Id.create(h3Address, Zone.class);

		if(zones.containsKey(zoneId)) {
			return Optional.of(zones.get(zoneId));
		} else {
			Optional<Zone> zone = H3Utils.createZone(h3Address, fromLatLong);
			if(zone.isPresent() && filter.test(zone.get())) {
				initZone(zone.get(), h3Address);
				return zone;
			} else {
				return Optional.empty();
			}
		}
	}

	private void initZone(Zone zone, long h3Address) {
		if(filter.test(zone)) {
			zones.put(zone.getId(), zone);
			for (Link link : network.getLinks().values()) {
				long linkH3Address = getH3Cell(link.getToNode().getCoord());

				if (linkH3Address == h3Address) {
					List<Link> links = zoneToLinksMap.computeIfAbsent(zone.getId(), id -> new ArrayList<>());
					links.add(link);
				}
			}
		}
	}

	private long getH3Cell(Coord coord) {
		long h3Address;
		if(coordH3Cache.containsKey(coord)) {
			h3Address = coordH3Cache.get(coord);
		} else {
			h3Address = H3Utils.getH3Cell(toLatLong.transform(coord), resolution);
			coordH3Cache.put(coord, h3Address);
		}
		return h3Address;
	}

	@Override
	public Optional<Zone> getZoneForLinkId(Id<Link> link) {
		return getZoneForCoord(network.getLinks().get(link).getToNode().getCoord());
	}

	@Override
	public Optional<Zone> getZoneForNodeId(Id<Node> nodeId) {
		return getZoneForCoord(network.getNodes().get(nodeId).getCoord());
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return zoneToLinksMap.getOrDefault(zone, Collections.emptyList());
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}
}
