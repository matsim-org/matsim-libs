/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.drt.analysis.zonal;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

/**
 * @author jbischoff
 * @author Michal Maciejewski (michalm)
 * @author Tilmann Schlenther (tschlenther)
 */
public class DrtZonalSystem {

	public static DrtZonalSystem createFromPreparedGeometries(Network network,
			Map<String, PreparedGeometry> geometries) {

		//geometries without links are skipped
		Map<String, List<Link>> linksByGeometryId = StreamEx.of(network.getLinks().values())
				.mapToEntry(l -> getGeometryIdForLink(l, geometries), l -> l)
				.filterKeys(Objects::nonNull)
				.grouping(toList());

		//the zonal system contains only zones that have at least one link
		List<DrtZone> zones = EntryStream.of(linksByGeometryId)
				.mapKeyValue((id, links) -> new DrtZone(id, geometries.get(id), links))
				.collect(toList());

		return new DrtZonalSystem(zones);
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
				.map(Entry::getKey)
				.orElse(null);
	}

	private final Map<String, DrtZone> zones;
	private final Map<Id<Link>, DrtZone> link2zone;

	public DrtZonalSystem(Collection<DrtZone> zones) {
		this.zones = zones.stream().collect(toMap(DrtZone::getId, z -> z));
		this.link2zone = zones.stream()
				.flatMap(zone -> zone.getLinks().stream().map(link -> Pair.of(link.getId(), zone)))
				.collect(toMap(Pair::getKey, Pair::getValue));
	}

	/**
	 * @param linkId
	 * @return the the {@code DrtZone} that contains the {@code linkId}. If the given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
	@Nullable
	public DrtZone getZoneForLinkId(Id<Link> linkId) {
		return link2zone.get(linkId);
	}

	/**
	 * @return the zones
	 */
	public Map<String, DrtZone> getZones() {
		return zones;
	}
}
