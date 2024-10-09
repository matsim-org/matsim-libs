package org.matsim.application.prepare.counts;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Wrapper class for an STRtree. Can be used to match objects, geometries for example, to an MATSim network.
 */
public final class NetworkIndex<T> {

	private final STRtree index = new STRtree();
	private final double range;
	private final GeometryFactory factory = new GeometryFactory();
	private final GeometryGetter<T> getter;
	private final List<BiPredicate<LinkGeometry, T>> filter = new ArrayList<>();
	/**
	 * Stores references to all records in the tree.
	 */
	private final Map<Id<Link>, LinkGeometry> records = new HashMap<>();
	private GeometryDistance<T> distance;

	/**
	 * Create network index from links in the network.
	 */
	public NetworkIndex(Network network, double range, GeometryGetter<T> getter) {
		this(network, new HashMap<>(), range, getter);
	}


	/**
	 * Create network index from links in the network with additional geometries.
	 */
	public NetworkIndex(Network network, Map<Id<Link>, Geometry> geometries, double range, GeometryGetter<T> getter) {

		this.range = range;
		this.getter = getter;
		// Standard geometric distance
		this.distance = (geom, toMatch) -> geom.distance(this.getter.getGeometry(toMatch));

		for (Link link : network.getLinks().values()) {
			Geometry geom = geometries.getOrDefault(link.getId(), this.link2LineString(link));
			LinkGeometry r = new LinkGeometry(link, geom);
			this.index.insert(r.geometry.getEnvelopeInternal(), r);
			this.records.put(link.getId(), r);
		}

		this.index.build();
	}

	/**
	 * Calculate the minimum hausdorff distance between two geometries.
	 * Unlike the classical distance, this uses the minimum instead of maximum of the two directed distances.
	 * This makes it more usable for geometries with different extent.
	 * This function may be used to compute the similarity of two line strings.
	 */
	public static double minHausdorffDistance(Geometry g1, Geometry g2) {

		DiscreteHausdorffDistance d1 = new DiscreteHausdorffDistance(g1, g2);
		double u = d1.orientedDistance();

		DiscreteHausdorffDistance d2 = new DiscreteHausdorffDistance(g2, g1);
		double v = d2.orientedDistance();

		return Math.min(u, v);
	}

	/**
	 * Calculates the angle of vector from v to u.
	 */
	public static double angle(Coordinate v, Coordinate u) {
		return Math.atan2(u.getY() - v.getY(), u.getX() - v.getX());
	}

	/**
	 * Angle between two line strings in radians -pi to pi.
	 */
	public static double angle(LineString u, LineString v) {

		double ux = u.getEndPoint().getCoordinate().getX() - u.getStartPoint().getCoordinate().getX();
		double uy = u.getEndPoint().getCoordinate().getY() - u.getStartPoint().getCoordinate().getY();

		double vx = v.getEndPoint().getCoordinate().getX() - v.getStartPoint().getCoordinate().getX();
		double vy = v.getEndPoint().getCoordinate().getY() - v.getStartPoint().getCoordinate().getY();

		double cross = ux * vy - uy * vx;
		double dot = ux * vx + uy * vy;

		return Math.atan2(cross, dot);
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
	 * Change the distance calculation.
	 */
	public NetworkIndex<T> setDistanceCalculator(GeometryDistance<T> distance) {
		this.distance = distance;
		return this;
	}

	/**
	 * Uses an STRtree to match an Object to a network link. Custom filters are applied to filter the query results.
	 * The closest link to the object, based on the distance function is returned.
	 *
	 * @param filter optional filter, only for this query.
	 */
	@SuppressWarnings("unchecked")
	public Link query(T toMatch, @Nullable BiPredicate<LinkGeometry, T> filter) {

		Geometry geometry = getter.getGeometry(toMatch);

		Envelope searchArea = geometry.buffer(this.range).getEnvelopeInternal();

		List<LinkGeometry> result = index.query(searchArea);

		if (result.isEmpty())
			return null;

		return getClosestCandidate(result, toMatch, filter);
	}

	/**
	 * See {@link #query(Object, BiPredicate)}.
	 */
	public Link query(T toMatch) {
		return query(toMatch, null);
	}

	/**
	 * Transforms a MATSim network link to a LineString Object.
	 */
	private LineString link2LineString(Link link) {

		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates);
	}

	/**
	 * Removes a Link from the index.
	 */
	public void remove(Link link) {
		LinkGeometry r = records.get(link.getId());
		index.remove(r.geometry.getEnvelopeInternal(), r);
	}

	private Link getClosestCandidate(List<LinkGeometry> result, T toMatch, BiPredicate<LinkGeometry, T> filter) {

		if (result.isEmpty())
			return null;

		if (result.size() == 1)
			return result.stream().findFirst().get().link;

		applyFilter(result, toMatch, filter);

		if (result.isEmpty())
			return null;

		Map<Link, Double> distances = result.stream().collect(Collectors.toMap(r -> r.link, r -> distance.computeDistance(r.geometry, toMatch)));
		Map.Entry<Link, Double> min = Collections.min(distances.entrySet(), Comparator.comparingDouble(Map.Entry::getValue));
		return min.getKey();
	}

	/**
	 * Add a Predicate to test if query results are a valid candidate. Should return false if element should NOT be returned.
	 */
	public void addLinkFilter(BiPredicate<LinkGeometry, T> filter) {
		this.filter.add(filter);
	}

	private void applyFilter(List<LinkGeometry> result, T toMatch, BiPredicate<LinkGeometry, T> filter) {

		outer:
		for (var it = result.iterator(); it.hasNext(); ) {
			LinkGeometry next = it.next();
			for (BiPredicate<LinkGeometry, T> predicate : this.filter) {
				if (!predicate.test(next, toMatch)) {
					it.remove();
					// No further filter should be checked
					continue outer;
				}
			}

			if (filter != null && !filter.test(next, toMatch))
				it.remove();
		}
	}

	/**
	 * Set the logic to get a geometry from the matching object.
	 */
	@FunctionalInterface
	public interface GeometryGetter<T> {

		Geometry getGeometry(T toMatch);
	}


	/**
	 * Logic to compute distance or similarity between two objects.
	 */
	@FunctionalInterface
	public interface GeometryDistance<T> {

		double computeDistance(Geometry geom, T toMatch);

	}

	/**
	 * Holding link and its geometry.
	 */
	public record LinkGeometry(Link link, Geometry geometry) { }
}

