package org.matsim.application.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Wrapper class for an STRtree. Can be used to match objects, geometries for example, to an MATSim network.
 */
public class NetworkIndex<T> {

	private final STRtree index = new STRtree();
	private final double range;
	private final GeometryFactory factory = new GeometryFactory();
	private final GeometryGetter<T> getter;
	private final List<BiPredicate<Link, T>> filter = new ArrayList<>();

	/**
	 * Stores references to all records in the tree.
	 */
	private final Map<Id<Link>, LinkGeometryRecord> records = new HashMap<>();

	/**
	 * TODO: docs
	 */
	public NetworkIndex(Network network, double range, GeometryGetter<T> getter) {
		this(network, new HashMap<>(), range, getter);
	}


	/**
	 * TODO docs
	 */
	public NetworkIndex(Network network, Map<Id<Link>, Geometry> geometries, double range, GeometryGetter<T> getter) {

		this.range = range;
		this.getter = getter;

		for (Link link : network.getLinks().values()) {
			Geometry geom = geometries.getOrDefault(link.getId(), this.link2LineString(link));
			LinkGeometryRecord r = new LinkGeometryRecord(link, geom);
			this.index.insert(r.geometry.getEnvelopeInternal(), r);
			this.records.put(link.getId(), r);
		}

		this.index.build();
	}

	/**
	 * Read network geometries that have been written with {@link org.matsim.contrib.sumo.SumoNetworkConverter}.
	 */
	public static Map<Id<Link>, Geometry> readGeometriesFromSumo(String path, MathTransform crs) throws IOException, TransformException {

		Map<Id<Link>, Geometry> result = new HashMap<>();

		GeometryFactory factory = new GeometryFactory();

		try (CSVParser csv = new CSVParser(IOUtils.getBufferedReader(path), CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

			for (CSVRecord r : csv) {
				String idAsString = r.get("LinkId");
				String raw = r.get("Geometry");

				LineString link = parseCoordinates(raw, factory);
				Id<Link> linkId = Id.createLinkId(idAsString);

				result.put(linkId, JTS.transform(link, crs));
			}
		}

		return result;
	}

	private static LineString parseCoordinates(String coordinateSequence, GeometryFactory factory) {

		String[] split = coordinateSequence.split("\\)");

		Coordinate[] coordinates = new Coordinate[split.length];

		for (int i = 0; i < split.length; i++) {
			String coord = split[i];
			int toRemove = coord.indexOf("(");

			String cleaned = coord.substring(toRemove + 1);

			String[] split1 = cleaned.split(",");

			Coordinate coordinate = new Coordinate();
			coordinate.setX(Double.parseDouble(split1[0]));
			coordinate.setY(Double.parseDouble(split1[1]));

			coordinates[i] = coordinate;
		}

		return factory.createLineString(coordinates);
	}

	/**
	 * Uses an STRtree to match an Object to a network link. Custom filters are applied to filter the query results.
	 * The closest link to the object, based on the Geometry distance function is returned.
	 */
	@SuppressWarnings("unchecked")
	public Link query(T toMatch) {

		Geometry geometry = getter.getGeometry(toMatch);

		Envelope searchArea = geometry.buffer(this.range).getEnvelopeInternal();

		List<LinkGeometryRecord> result = index.query(searchArea);

		if (result.isEmpty()) return null;

		return getClosestCandidate(result, toMatch);
	}

	/**
	 * Transforms a MATSim network link to a LineString Object.
	 */
	public LineString link2LineString(Link link) {

		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates);
	}

	/**
	 * Removes a Link from the index.
	 */
	public void remove(Link link) {
		LinkGeometryRecord r = records.get(link.getId());
		index.remove(r.geometry.getEnvelopeInternal(), r);
	}

	private Link getClosestCandidate(List<LinkGeometryRecord> result, T toMatch) {

		if (result.isEmpty()) return null;
		if (result.size() == 1) return result.stream().findFirst().get().link;

		applyFilter(result, toMatch);

		if (result.isEmpty())
			return null;

		Map<Link, Double> distances = result.stream()
			.collect(Collectors.toMap(r -> r.link, r -> r.geometry.distance(this.getter.getGeometry(toMatch))));

		Double min = Collections.min(distances.values());

		for (Map.Entry<Link, Double> entry : distances.entrySet()) {
			if (entry.getValue().doubleValue() == min.doubleValue()) {
				return entry.getKey();
			}
		}

		return null;
	}

	/**
	 * Add a Predicate to test if query results are a valid candidate. Should return false if element should NOT be returned.
	 */
	public void addLinkFilter(BiPredicate<Link, T> filter) {
		this.filter.add(filter);
	}

	private void applyFilter(List<LinkGeometryRecord> result, T toMatch) {

		for (var it = result.iterator(); it.hasNext(); ) {

			LinkGeometryRecord next = it.next();
			Link link = next.link;
			for (BiPredicate<Link, T> predicate : this.filter) {
				if (!predicate.test(link, toMatch)) {
					it.remove();
					break;
				}
			}
		}
	}

	/**
	 * Set the logic to get a geometry from the matching object.
	 */
	@FunctionalInterface
	public interface GeometryGetter<T> {

		Geometry getGeometry(T toMatch);
	}

	private record LinkGeometryRecord(Link link, Geometry geometry) {
	}
}

