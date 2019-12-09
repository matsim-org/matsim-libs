package org.matsim.contrib.osm.networkReader;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SupersonicOsmNetworkReaderIT {

	private static final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32631");

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test_andorra() {

		Network network = SupersonicOsmNetworkReader.builder()
				.coordinateTransformation(coordinateTransformation)
				.build()
				.read(Paths.get(utils.getInputDirectory()).resolve("andorra-latest.osm.pbf"));

		Network expectedResult = NetworkUtils.createNetwork();
		new MatsimNetworkReader(expectedResult).readFile(Paths.get(utils.getInputDirectory()).resolve("expected-result.xml.gz").toString());

		// check that all element from expected result are in tested network
		for (Link link : expectedResult.getLinks().values()) {
			Link testLink = network.getLinks().get(link.getId());
			assertNotNull(testLink);
			testLinksAreEqual(link, testLink);
		}

		for (Node node : expectedResult.getNodes().values()) {
			Node testNode = network.getNodes().get(node.getId());
			assertNotNull(testNode);
			testNodesAreEqual(node, testNode);
		}

		// also check the other way around, to make sure there are no extra elements in the network
		for (Link link : network.getLinks().values()) {
			Link expectedLink = expectedResult.getLinks().get(link.getId());
			assertNotNull(expectedLink);
		}

		for (Node node : network.getNodes().values()) {
			Node expectedNode = expectedResult.getNodes().get(node.getId());
			assertNotNull(expectedNode);
		}
	}

	private void testLinksAreEqual(Link expected, Link actual) {

		expected.getAllowedModes().forEach(mode -> assertTrue(actual.getAllowedModes().contains(mode)));
		assertEquals(expected.getCapacity(), actual.getCapacity(), 0.001);
		assertEquals(expected.getFlowCapacityPerSec(), actual.getFlowCapacityPerSec(), 0.001);
		assertEquals(expected.getFreespeed(), actual.getFreespeed(), 0.001);
		assertEquals(expected.getLength(), actual.getLength(), 0.001);
		assertEquals(expected.getNumberOfLanes(), actual.getNumberOfLanes(), 0.001);
	}

	private void testNodesAreEqual(Node expected, Node actual) {
		assertEquals(expected.getCoord(), actual.getCoord());
	}
}
