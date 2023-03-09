package org.matsim.application.prepare.counts;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

import java.util.HashMap;
import java.util.List;

public class NetworkIndex {

	private final STRtree index = new STRtree();
	private final double range;
	private final GeometryFactory factory = new GeometryFactory();


	public NetworkIndex(Network network, double range) {

		this.range = range;

		for (Link link : network.getLinks().values()) {
			Envelope env = getLinkEnvelope(link);
			index.insert(env, link);
		}

		index.build();
	}

	@SuppressWarnings("unchecked")
	public Link query(MatchingPoint toMatch) {

		Geometry geometry = toMatch.getGeometry();

		Envelope searchArea = geometry.buffer(this.range).getEnvelopeInternal();

		List<Link> result = index.query(searchArea);

		if (result.isEmpty()) return null;

		HashMap<Link, Geometry> resultMap = new HashMap<>();
		for (Link link : result) {

			LineString ls = this.link2LineString(link);
			resultMap.put(link, ls);
		}

		return toMatch.getClosestCandidate(resultMap);
	}

	public Envelope getLinkEnvelope(Link link) {
		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates).getEnvelopeInternal();
	}

	public LineString link2LineString(Link link) {

		Coord from = link.getFromNode().getCoord();
		Coord to = link.getToNode().getCoord();
		Coordinate[] coordinates = {MGC.coord2Coordinate(from), MGC.coord2Coordinate(to)};

		return factory.createLineString(coordinates);
	}

	public void remove(Link link) {
		Envelope env = getLinkEnvelope(link);
		index.remove(env, link);
	}
}

