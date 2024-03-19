package org.matsim.contrib.common.zones;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public final class ZoneSystemUtils {


	private ZoneSystemUtils() {}

	public static ZoneSystem createFromPreparedGeometries(Network network, Map<String, PreparedGeometry> geometries) {

		//geometries without links are skipped
		Map<String, List<Link>> linksByGeometryId = StreamEx.of(network.getLinks().values())
			.mapToEntry(l -> getGeometryIdForLink(l, geometries), l -> l)
			.filterKeys(Objects::nonNull)
			.grouping(toList());

		//the zonal system contains only zones that have at least one link
		List<Zone> zones = EntryStream.of(linksByGeometryId)
			.mapKeyValue((id, links) -> new ZoneImpl(Id.create(id, Zone.class), geometries.get(id), links))
			.collect(toList());

		return new ZoneSystemImpl(zones);
	}

	/**
	 * @param link
	 * @return the the {@code PreparedGeometry} that contains the {@code linkId}.
	 * If a given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
	@Nullable
	private static String getGeometryIdForLink(Link link, Map<String, PreparedGeometry> geometries) {
		Point linkCoord = MGC.coord2Point(link.getToNode().getCoord());
		return geometries.entrySet()
			.stream()
			.filter(e -> e.getValue().intersects(linkCoord))
			.findAny()
			.map(Map.Entry::getKey)
			.orElse(null);
	}

	public static Id<Zone> createZoneId(String id) {
		return Id.create(id, Zone.class);
	}

	public static Id<Zone> createZoneId(long id) {
		return Id.create(id, Zone.class);
	}
}
