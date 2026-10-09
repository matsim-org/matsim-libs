package org.matsim.contrib.sumo;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SumoNetworkHandlerTest {


	@Test
	void read() throws Exception {

        URL resource = Resources.getResource("osm.net.xml");

        SumoNetworkHandler handler = SumoNetworkHandler.read(new File(resource.toURI()));

        assert handler.edges.containsKey("-160346478#3"): "Must contain specific edge";
    }

	/**
	 * Pins the part of the handler that post-processing outside this package relies on:
	 * the OSM way id behind an edge, the exported tags and the shape. Everything here is
	 * reachable through public API only, without touching package-private state.
	 */
	@Test
	void exposesEdgeOriginAndShape() throws Exception {

		URL resource = Resources.getResource("bike-types.net.xml");

		SumoNetworkHandler handler = SumoNetworkHandler.read(new File(resource.toURI()));

		SumoNetworkHandler.Edge edge = handler.getEdges().get("1001");
		assertNotNull(edge, "Fixture must contain edge 1001");

		assertEquals("1001", edge.getId());
		assertEquals("highway.footway", edge.getType());
		assertEquals("1001", edge.getOrigId(), "origId must be the plain OSM way id");
		assertEquals("yes", edge.getAttributes().get("bicycle"));
		assertEquals("asphalt", edge.getAttributes().get("surface"));

		// the reverse edge of the same way carries the same origId, only the id is negated
		SumoNetworkHandler.Edge reverse = handler.getEdges().get("-1001");
		assertNotNull(reverse, "Fixture must contain the reverse edge");
		assertEquals(edge.getOrigId(), reverse.getOrigId());

		// shape points are raw network coordinates; createCoord removes the net offset
		assertNotNull(edge.getShape(), "A straight edge has an empty shape, never null");
		Coord coord = handler.createCoord(new double[]{1000, 2000});
		assertTrue(Double.isFinite(coord.getX()) && Double.isFinite(coord.getY()));
	}
}