package org.matsim.contrib.common.zones;

import com.google.common.base.Preconditions;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.common.zones.io.ZoneShpReader;
import org.matsim.contrib.common.zones.io.ZoneXmlReader;
import org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystem;
import org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3ZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.common.zones.util.ZoneFinder;
import org.matsim.contrib.common.zones.util.ZoneFinderImpl;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * @author nkuehnel / MOIA
 */
public final class ZoneSystemUtils {


	public static final String THE_GEOM = "the_geom";

	private ZoneSystemUtils() {}

	public static ZoneSystem createZoneSystem(Network network, ZoneSystemParams zoneSystemParams) {
		return createZoneSystem(null, network, zoneSystemParams, null, zone -> true);
	}

	public static ZoneSystem createZoneSystem(URL context, Network network, ZoneSystemParams zoneSystemParams) {
		return createZoneSystem(context, network, zoneSystemParams, null, zone -> true);
	}

	public static ZoneSystem createZoneSystem(@Nullable URL context, @Nonnull Network network,
											  @Nonnull ZoneSystemParams zoneSystemParams, @Nullable String crs,
											  Predicate<Zone> zoneFilter) {

		final ZoneSystem zoneSystem = switch (zoneSystemParams.getName()) {
			case GISFileZoneSystemParams.SET_NAME -> {
				Preconditions.checkNotNull(((GISFileZoneSystemParams) zoneSystemParams).zonesShapeFile);
				Preconditions.checkNotNull(context);
				URL url = ConfigGroup.getInputFileURL(context, ((GISFileZoneSystemParams) zoneSystemParams).zonesShapeFile);
				Collection<SimpleFeature> features = GeoFileReader.getAllFeatures(url);
				yield ZoneSystemUtils.createFromFeatures(network, features, zoneFilter);
			}
			case SquareGridZoneSystemParams.SET_NAME -> {
				Preconditions.checkNotNull(((SquareGridZoneSystemParams) zoneSystemParams).cellSize);
                yield new SquareGridZoneSystem(network, ((SquareGridZoneSystemParams) zoneSystemParams).cellSize, zoneFilter);
			}
			case H3GridZoneSystemParams.SET_NAME -> {
				Preconditions.checkNotNull(((H3GridZoneSystemParams) zoneSystemParams).h3Resolution);
				Preconditions.checkNotNull(crs);
				yield new H3ZoneSystem(crs, ((H3GridZoneSystemParams) zoneSystemParams).h3Resolution, network, zoneFilter);
			}
			case GeometryFreeZoneSystemParams.SET_NAME -> new GeometryFreeZoneSystem(network);
			default -> throw new IllegalStateException("Unexpected value: " + zoneSystemParams.getName());
		};
		return zoneSystem;
	}

	public static ZoneSystem createFromFeatures(Network network, Collection<SimpleFeature> features, Predicate<Zone> zoneFilter) {

		Map<String, PreparedFeature> featureById = StreamEx.of(features.stream())
			.mapToEntry(SimpleFeature::getID, sf -> new PreparedFeature(sf, new PreparedPolygon((Polygonal) sf.getDefaultGeometry())))
			.toMap();

		//geometries without links are skipped
		Map<String, List<Link>> linksByGeometryId = StreamEx.of(network.getLinks().values())
			.mapToEntry(l -> getGeometryIdForLink(l, featureById), l -> l)
			.filterKeys(Objects::nonNull)
			.grouping(toList());

		//the zonal system contains only zones that have at least one link
		Map<Id<Zone>, Zone> zones = EntryStream.of(linksByGeometryId)
			.mapKeyValue((id, links) -> {
				PreparedFeature preparedFeature = featureById.get(id);
				ZoneImpl zone = new ZoneImpl(Id.create(id, Zone.class), preparedFeature.preparedPolygon, null);
				for (Property attribute : preparedFeature.sf.getProperties()) {
					String attributeName = attribute.getName().toString();
					Object att = preparedFeature.sf().getAttribute(attributeName);
					if(!attributeName.equals(THE_GEOM) && att != null) {
						zone.getAttributes().putAttribute(attributeName, att);
					}
				}
				return zone;
            })
			.filter(zoneFilter)
			.collect(IdCollectors.toIdMap(Zone.class, Identifiable::getId, zone -> zone));

		return new ZoneSystemImpl(zones.values(), new ZoneFinderImpl(zones), network);
	}

	private record PreparedFeature(SimpleFeature sf, PreparedPolygon preparedPolygon){}

	/**
	 * @param link
	 * @return the the {@code PreparedGeometry} that contains the {@code linkId}.
	 * If a given link's {@code Coord} borders two or more cells, the allocation to a cell is random.
	 * Result may be null in case the given link is outside of the service area.
	 */
	@Nullable
	private static String getGeometryIdForLink(Link link, Map<String, PreparedFeature> features) {
		Point linkCoord = MGC.coord2Point(link.getToNode().getCoord());
		return features.entrySet()
			.stream()
			.filter(e -> e.getValue().preparedPolygon.intersects(linkCoord))
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

	// if CRSs of the network and zones are different, zoneFinder should convert between CRSs
	public static IdMap<Link, Zone> createLinkToZoneMap(Network network, ZoneFinder zoneFinder) {
		return EntryStream.of(network.getLinks())
			.mapValues(link -> zoneFinder.findZone(link.getToNode().getCoord()))
			.filterValues(Optional::isPresent)
			.mapValues(Optional::get)
			.collect(IdCollectors.toIdMap(Link.class, Map.Entry::getKey, Map.Entry::getValue));
	}

	public static IdMap<Node, Zone> createNodeToZoneMap(Network network, ZoneFinder zoneFinder) {
		return EntryStream.of(network.getNodes())
			.mapValues(node -> zoneFinder.findZone(node.getCoord()))
			.filterValues(Optional::isPresent)
			.mapValues(Optional::get)
			.collect(IdCollectors.toIdMap(Node.class, Map.Entry::getKey, Map.Entry::getValue));

	}

	public static Set<Zone> filterZonesWithNodes(Collection<? extends Node> nodes, ZoneSystem zoneSystem) {
		return nodes.stream().map(node -> zoneSystem.getZoneForNodeId(node.getId())).filter(Optional::isPresent).map(Optional::get).collect(toSet());
	}

	public static List<Node> selectNodesWithinArea(Collection<? extends Node> nodes, List<PreparedGeometry> areaGeoms) {
		return nodes.stream().filter(node -> {
			Point point = MGC.coord2Point(node.getCoord());
			return areaGeoms.stream().anyMatch(serviceArea -> serviceArea.intersects(point));
		}).collect(toList());
	}

	public static Map<Zone, Node> computeMostCentralNodes(Collection<? extends Node> nodes, ZoneSystem zoneSystem) {
		BinaryOperator<Node> chooseMoreCentralNode = (n1, n2) -> {
			Zone zone = zoneSystem.getZoneForNodeId(n1.getId()).orElseThrow();
			return DistanceUtils.calculateSquaredDistance(n1, zone) <= DistanceUtils.calculateSquaredDistance(n2,
					zone) ? n1 : n2;
		};
		return nodes.stream()
				.map(n -> Pair.of(n, zoneSystem.getZoneForNodeId(n.getId()).orElseThrow()))
				.collect(toMap(Pair::getValue, Pair::getKey, chooseMoreCentralNode));
	}

	public static IdMap<Zone, List<Zone>> initZonesByDistance(Map<Id<Zone>, Zone> zones) {
		IdMap<Zone, List<Zone>> zonesByDistance = new IdMap<>(Zone.class);
		for (final Zone currentZone : zones.values()) {
			List<Zone> sortedZones = zones.values()
					.stream()
					.sorted(Comparator.comparing(z -> DistanceUtils.calculateSquaredDistance(currentZone, z)))
					.collect(Collectors.toList());
			zonesByDistance.put(currentZone.getId(), sortedZones);
		}
		return zonesByDistance;
	}
}
