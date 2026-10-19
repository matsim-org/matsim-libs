/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.router.AttributeBasedStopFinder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import com.google.common.collect.ImmutableMap;

/**
 * @author nkuehnel / MOIA
 */
class DrtServiceAreasTest {
	private static final String ATTRIBUTE = AttributeBasedStopFinder.FACILITY_STOP_NETWORKS_ATTRIBUTE;
	private static final String CRS = "EPSG:32633";

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testStopNetworksAreTheUnionOfTheContainingAreas() {
		// the western half is served in both regimes, the eastern half only in the morning
		DrtServiceAreas serviceAreas = serviceAreas(area(0, 0, 500, 1000, "morning,evening"),
				area(0, 0, 1000, 1000, "morning"));

		assertThat(serviceAreas.stopNetworksAt(new Coord(250, 500))).containsExactlyInAnyOrder("morning", "evening");
		assertThat(serviceAreas.stopNetworksAt(new Coord(750, 500))).containsExactly("morning");
		assertThat(serviceAreas.stopNetworksAt(new Coord(1500, 500))).isEmpty();

		assertThat(serviceAreas.covers(new Coord(750, 500))).isTrue();
		assertThat(serviceAreas.covers(new Coord(1500, 500))).isFalse();
	}

	@Test
	void testAreaWithoutStopNetworksOnlyContributesToTheServedArea() {
		DrtServiceAreas serviceAreas = serviceAreas(area(0, 0, 1000, 1000, null));

		assertThat(serviceAreas.covers(new Coord(500, 500))).isTrue();
		assertThat(serviceAreas.stopNetworksAt(new Coord(500, 500))).isEmpty();
	}

	@Test
	void testIntersectsIsTheUnionOfAllAreas() {
		// two disjoint areas, so only their union describes the served area
		DrtServiceAreas serviceAreas = serviceAreas(area(0, 0, 500, 1000, "morning"),
				area(2000, 0, 2500, 1000, "evening"));

		assertThat(serviceAreas.intersects(geometry(400, 400, 600, 600))).isTrue();
		assertThat(serviceAreas.intersects(geometry(1900, 400, 2100, 600))).isTrue();
		// a zone between the two areas is not served, even though it lies between them
		assertThat(serviceAreas.intersects(geometry(1000, 0, 1500, 1000))).isFalse();
		// a zone which only touches an area intersects it, unlike a coordinate on the boundary, which is not covered
		assertThat(serviceAreas.intersects(geometry(500, 400, 700, 600))).isTrue();
		assertThat(serviceAreas.covers(new Coord(500, 500))).isFalse();
	}

	@Test
	void testMissingAttributeIsReported() {
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().setName("serviceAreas")
				.setCrs(MGC.getCRS(CRS))
				.addAttribute("someOtherAttribute", String.class)
				.create();
		SimpleFeature feature = factory.createPolygon(square(0, 0, 1000, 1000), Map.of(), "area");

		assertThatThrownBy(() -> DrtServiceAreas.fromFeatures(List.of(feature), ATTRIBUTE)).hasMessageContaining(
				ATTRIBUTE).hasMessageContaining("someOtherAttribute");
	}

	@Test
	void testCreateStopNetworkTakesTheLinksInsideTheAreas() {
		Network network = network();
		DrtServiceAreas serviceAreas = serviceAreas(area(0, 0, 500, 1000, "morning,evening"),
				area(0, 0, 1000, 1000, "morning"));

		DrtStopNetwork stopNetwork = serviceAreas.createStopNetwork(network);

		// the link to the node outside the areas does not become a stop
		assertThat(stopNetwork.getDrtStops().keySet()).containsExactlyInAnyOrder(
				Id.create("west", DrtStopFacility.class), Id.create("east", DrtStopFacility.class));
		assertThat(stopNetworks(stopNetwork, "west")).containsExactlyInAnyOrder("morning", "evening");
		assertThat(stopNetworks(stopNetwork, "east")).containsExactly("morning");

		// the link attributes are the source of the stop attributes, so they must not be touched
		assertThat(network.getLinks().get(Id.createLinkId("west")).getAttributes().getAttribute(ATTRIBUTE)).isNull();
	}

	@Test
	void testCreateStopNetworkFailsIfNoLinkIsInsideTheAreas() {
		DrtServiceAreas serviceAreas = serviceAreas(area(10_000, 10_000, 11_000, 11_000, "morning"));

		assertThatThrownBy(() -> serviceAreas.createStopNetwork(network())).hasMessageContaining("service areas");
	}

	@Test
	void testTagKeepsAllStopsAndMergesTheExistingAttribute() {
		DrtStopFacility west = stop("west", new Coord(250, 500), "manuallyTagged");
		DrtStopFacility east = stop("east", new Coord(750, 500), null);
		DrtStopFacility outside = stop("outside", new Coord(1500, 500), null);
		DrtStopNetwork stopNetwork = () -> ImmutableMap.of(west.getId(), west, east.getId(), east, outside.getId(),
				outside);

		DrtStopNetwork tagged = serviceAreas(area(0, 0, 500, 1000, "morning,evening"),
				area(0, 0, 1000, 1000, "morning")).tag(stopNetwork);

		assertThat(tagged.getDrtStops().keySet()).containsExactlyInAnyOrderElementsOf(
				stopNetwork.getDrtStops().keySet());
		assertThat(stopNetworks(tagged, "west")).containsExactlyInAnyOrder("manuallyTagged", "morning", "evening");
		assertThat(stopNetworks(tagged, "east")).containsExactly("morning");
		// a stop outside all areas is kept unchanged, so a service regime without stopNetwork still serves it
		assertThat(stopNetworks(tagged, "outside")).isEmpty();
	}

	@Test
	void testFromFile() throws MalformedURLException {
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().setName("serviceAreas")
				.setCrs(MGC.getCRS(CRS))
				// column names of shapefiles are limited to 10 characters, hence the short name
				.addAttribute("networks", String.class)
				.create();
		Path file = Path.of(utils.getOutputDirectory()).resolve("serviceAreas.shp").toAbsolutePath();
		GeoFileWriter.writeGeometries(
				List.of(factory.createPolygon(square(0, 0, 500, 1000), Map.of("networks", "morning,evening"), "west"),
						factory.createPolygon(square(500, 0, 1000, 1000), Map.of("networks", "morning"), "east")),
				file.toString());

		DrtServiceAreas serviceAreas = DrtServiceAreas.fromFile(file.toUri().toURL(), "networks");

		assertThat(serviceAreas.stopNetworksAt(new Coord(250, 500))).containsExactlyInAnyOrder("morning", "evening");
		assertThat(serviceAreas.stopNetworksAt(new Coord(750, 500))).containsExactly("morning");
		assertThat(serviceAreas.covers(new Coord(1500, 500))).isFalse();
	}

	private static DrtServiceAreas serviceAreas(SimpleFeature... areas) {
		return DrtServiceAreas.fromFeatures(List.of(areas), ATTRIBUTE);
	}

	private static SimpleFeature area(double minX, double minY, double maxX, double maxY, String stopNetworks) {
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().setName("serviceAreas")
				.setCrs(MGC.getCRS(CRS))
				.addAttribute(ATTRIBUTE, String.class)
				.create();
		Map<String, Object> attributes = stopNetworks == null ? Map.of() : Map.of(ATTRIBUTE, stopNetworks);
		return factory.createPolygon(square(minX, minY, maxX, maxY), attributes, "area");
	}

	private static Geometry geometry(double minX, double minY, double maxX, double maxY) {
		return (Geometry)area(minX, minY, maxX, maxY, null).getDefaultGeometry();
	}

	private static Coordinate[] square(double minX, double minY, double maxX, double maxY) {
		return new Coordinate[] { new Coordinate(minX, minY), new Coordinate(maxX, minY), new Coordinate(maxX, maxY),
				new Coordinate(minX, maxY) };
	}

	/**
	 * Three links, whose toNodes lie in the west, in the east and outside of the areas used in the tests.
	 */
	private static Network network() {
		Network network = NetworkUtils.createNetwork();
		Node origin = NetworkUtils.createAndAddNode(network, Id.createNodeId("origin"), new Coord(250, 500));
		Node west = NetworkUtils.createAndAddNode(network, Id.createNodeId("west"), new Coord(250, 500));
		Node east = NetworkUtils.createAndAddNode(network, Id.createNodeId("east"), new Coord(750, 500));
		Node outside = NetworkUtils.createAndAddNode(network, Id.createNodeId("outside"), new Coord(1500, 500));
		NetworkUtils.createAndAddLink(network, Id.createLinkId("west"), origin, west, 100, 10, 1000, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("east"), origin, east, 100, 10, 1000, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("outside"), origin, outside, 100, 10, 1000, 1);
		return network;
	}

	private static DrtStopFacility stop(String id, Coord coord, String stopNetworks) {
		AttributesImpl attributes = new AttributesImpl();
		if (stopNetworks != null) {
			attributes.putAttribute(ATTRIBUTE, stopNetworks);
		}
		return new DrtStopFacilityImpl(Id.create(id, DrtStopFacility.class), Id.createLinkId(id), coord, attributes);
	}

	private static Iterable<String> stopNetworks(DrtStopNetwork stopNetwork, String stopId) {
		DrtStopFacility stop = stopNetwork.getDrtStops().get(Id.create(stopId, DrtStopFacility.class));
		return AttributeBasedStopFinder.parseStopNetworks(stop);
	}
}
