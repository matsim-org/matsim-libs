package org.matsim.application.prepare.counts;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

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
	private final GeometryGetter getter;
	private final List<BiPredicate<Link, T>> filter = new ArrayList<>();

	public NetworkIndex(Network network, double range, GeometryGetter getter) {

		this.range = range;
		this.getter = getter;

		for (Link link : network.getLinks().values()) {
			Envelope env = getLinkEnvelope(link);
			index.insert(env, link);
		}

		index.build();
	}

	/**
	 * Uses an STRtree to match an Object to a network link. Custom filters are applied to filter the query results.
	 * The closest link to the object, based on the Geometry distance function is returned.
	 */
	@SuppressWarnings("unchecked")
	public Link query(T toMatch) {

		Geometry geometry = getter.getGeometry(toMatch);

		Envelope searchArea = geometry.buffer(this.range).getEnvelopeInternal();

		List<Link> result = index.query(searchArea);

		if (result.isEmpty()) return null;

		Map<Link, Geometry> resultMap = new HashMap<>();
		for (Link link : result) {

			LineString ls = this.link2LineString(link);
			resultMap.put(link, ls);
		}

		return getClosestCandidate(resultMap, toMatch);
	}

	/**
	 * Returns the envelope of an MATSim network link.
	 */
	public Envelope getLinkEnvelope(Link link) {
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates).getEnvelopeInternal();
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
		Envelope env = getLinkEnvelope(link);
		index.remove(env, link);
	}

	private Link getClosestCandidate(Map<Link, Geometry> result, T toMatch) {

		if (result.isEmpty()) return null;
		if (result.size() == 1) return result.keySet().stream().findFirst().get();

		applyFilter(result, toMatch);

		if (result.isEmpty())
			return null;

		Map<Link, Double> distances = result.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, r -> r.getValue().distance(getter.getGeometry(toMatch))));

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

	private void applyFilter(Map<Link, Geometry> result, T toMatch) {

		for (var it = result.entrySet().iterator(); it.hasNext(); ) {

			Map.Entry<Link, Geometry> next = it.next();
			Link link = next.getKey();
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
}

