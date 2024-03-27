package org.matsim.contrib.common.zones;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.io.ZoneShpReader;
import org.matsim.contrib.common.zones.io.ZoneXmlReader;
import org.matsim.contrib.common.zones.util.ZoneFinderImpl;
import org.matsim.core.utils.geometry.geotools.MGC;

import javax.annotation.Nullable;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public final class ZoneSystemUtils {


	private ZoneSystemUtils() {}

	public static ZoneSystem createFromPreparedGeometries(Network network, Map<String, PreparedPolygon> geometries) {

		//geometries without links are skipped
		Map<String, List<Link>> linksByGeometryId = StreamEx.of(network.getLinks().values())
			.mapToEntry(l -> getGeometryIdForLink(l, geometries), l -> l)
			.filterKeys(Objects::nonNull)
			.grouping(toList());

		//the zonal system contains only zones that have at least one link
		Map<Id<Zone>, Zone> zones = EntryStream.of(linksByGeometryId)
			.mapKeyValue((id, links) -> new ZoneImpl(Id.create(id, Zone.class), geometries.get(id), null))
			.collect(IdCollectors.toIdMap(Zone.class, Identifiable::getId, zone -> zone));


		return new ZoneSystemImpl(zones.values(), new ZoneFinderImpl(zones), network);
	}

	/**
	 * @param link
	 * @return the the {@code PreparedGeometry} that contains the {@code linkId}.
	 * If a given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
				@Nullable
				private static String getGeometryIdForLink(Link link, Map<String, PreparedPolygon> geometries) {
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

	public static Map<Id<Zone>, Zone> readZones(String zonesXmlFile, String zonesShpFile) {
		try {
			return readZones(new File(zonesXmlFile).toURI().toURL(), new File(zonesShpFile).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Map<Id<Zone>, Zone> readZones(URL zonesXmlUrl, URL zonesShpUrl) {
		ZoneXmlReader xmlReader = new ZoneXmlReader();
		xmlReader.readURL(zonesXmlUrl);
		Map<Id<Zone>, Zone> zones = xmlReader.getZones();

		ZoneShpReader shpReader = new ZoneShpReader(zones);
		shpReader.readZones(zonesShpUrl);
		return zones;
	}
}
