package org.matsim.contrib.emissions.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.utils.TestUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FastEmissionGridAnalyzerTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void rasterNetwork_singleLink() {

        final var cellSize = 10.;
        final var cellArea = cellSize * cellSize;
        final var emissionPerLink = 20.;
        final var linkLength = 99;
        final var expectedCellNumber = (int) (linkLength / cellSize) + 1;

        var network = NetworkUtils.createNetwork(new NetworkConfigGroup());
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(linkLength, 0));
        var link = network.getFactory().createLink(Id.createLinkId("link"), node1, node2);
        network.addNode(node1);
        network.addNode(node2);
        network.addLink(link);

        var emissions = Map.of(link.getId(), emissionPerLink);

        var raster = FastEmissionGridAnalyzer.rasterizeNetwork(network, emissions, cellSize);

        assertEquals(expectedCellNumber, raster.getXLength());
        assertEquals(1, raster.getYLength());

        raster.forEachIndex((xi, yi, value) -> assertEquals(emissionPerLink / expectedCellNumber / cellArea, value, 0.00000001));
    }

	@Test
	void rasterNetwork_singleLinkWithBackwardsOrientation() {

        final var cellSize = 10.;
        final var cellArea = cellSize * cellSize;
        final var emissionPerLink = 20.;
        final var linkLength = 99;
        final var expectedCellNumber = (int) (linkLength / cellSize) + 1;

        var network = NetworkUtils.createNetwork(new NetworkConfigGroup());
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(linkLength, 0));
        var link = network.getFactory().createLink(Id.createLinkId("link"), node2, node1);
        network.addNode(node2);
        network.addNode(node1);
        network.addLink(link);

        var emissions = Map.of(link.getId(), emissionPerLink);

        var raster = FastEmissionGridAnalyzer.rasterizeNetwork(network, emissions, cellSize);

        assertEquals(expectedCellNumber, raster.getXLength());
        assertEquals(1, raster.getYLength());

        raster.forEachIndex((xi, yi, value) -> assertEquals(emissionPerLink / expectedCellNumber / cellArea, value, 0.00000001));
    }

	@Test
	void rasterNetwork_twoLinks() {

        final var cellSize = 10.;
        final var cellArea = cellSize * cellSize;
        final var emissionPerLink1 = 20.;
        final var emissionPerLink2 = 10.;
        final var linkLength = 99;
        final var expectedCellNumber = (int) (linkLength / cellSize) + 1;

        var network = NetworkUtils.createNetwork(new NetworkConfigGroup());
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(linkLength, 0));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(0, linkLength));
        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node1, node3);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addLink(link1);
        network.addLink(link2);

        var emissions = Map.of(link1.getId(), emissionPerLink1, link2.getId(), emissionPerLink2);

        var raster = FastEmissionGridAnalyzer.rasterizeNetwork(network, emissions, cellSize);

        assertEquals(expectedCellNumber, raster.getXLength());
        assertEquals(expectedCellNumber, raster.getYLength());

        raster.forEachIndex((xi, yi, value) -> {

            if (xi == 0 && yi == 0) {
                assertEquals((emissionPerLink1 + emissionPerLink2) / (expectedCellNumber * cellArea), value, 0.00001);
            } else if (xi == 0) {
                assertEquals(emissionPerLink2 / expectedCellNumber / cellArea, value, 0.0000001);
            } else if (yi == 0) {
                assertEquals(emissionPerLink1 / expectedCellNumber / cellArea, value, 0.000001);
            } else {
                assertEquals(0.0, value, 0.0001);
            }
        });

    }

	@Test
	void blur_rasterWithSingleValue() {

        final var initialValue = 10.;
        final var EPSILON = 0.000000001;
        // create a 5x5 raster
        var bounds = new Raster.Bounds(0, 0, 49, 49);
        var raster = new Raster(bounds, 10);
        // put a value into the center of the raster
        raster.adjustValueForIndex(2, 2, initialValue);

        // raster with a value of one which means only the surrounding pixels of the center receive values
        var blurredRaster = FastEmissionGridAnalyzer.blur(raster, 1);

        blurredRaster.forEachIndex((xi, yi, value) -> {

            // the value should be spread over 9 pixels. The center point should be 1/4 of the original value
            // the horizontally and vertically adjacent pixels should be 1/8 of the original value
            // the remaining pixels with values should be 1/16 of the original value
            // all other pixels should have a value of 0.0
            if (xi == 2 && yi == 2) assertEquals(2.5, value, EPSILON);
            else if ((xi == 1 || xi == 3) && yi == 2) assertEquals(1.25, value, EPSILON);
            else if (xi == 2 && (yi == 1 || yi == 3)) assertEquals(1.25, value, EPSILON);
            else if ((xi == 1 || xi == 3) && (yi == 1 || yi == 3)) assertEquals(0.625, value, EPSILON);
            else assertEquals(0.0, value, EPSILON);

        });
    }

	@Test
	void processLinkEmissions_twoLinks() {

        var network = NetworkUtils.createNetwork(new NetworkConfigGroup());
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 49));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(99, 49));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(49, 0));
        var node4 = network.getFactory().createNode(Id.createNodeId("node4"), new Coord(49, 99));

        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node3, node4);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addNode(node4);
        network.addLink(link1);
        network.addLink(link2);

        var emissions = Map.of(link1.getId(), 20., link2.getId(), 10.);

        var blurredRaster = FastEmissionGridAnalyzer.processLinkEmissions(emissions, network, 10, 1);

        var valueAtIntersection = blurredRaster.getValueByIndex(4, 4);
        assertEquals(0.015, valueAtIntersection, 0.00000001);

        blurredRaster.forEachIndex((xi, yi, value) -> {

            // all other values should be smaller than at the intersection
            if (xi != 4 && yi != 4) assertTrue(valueAtIntersection > value);
        });
    }

	@Test
	void processEventsFile() {

        final var networkUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "network.xml");
        final var emissionEvents = Paths.get(utils.getOutputDirectory()).resolve("emission.events.xml.gz");
        final var biggestExpectedValue = 6.056547619047618E-6;

        var network = NetworkUtils.readNetwork(networkUrl.toString());
        TestUtils.writeWarmEventsToFile(emissionEvents, network, Pollutant.NOx, 10, 1, 1);

        var rasterMap = FastEmissionGridAnalyzer.processEventsFile(emissionEvents.toString(), network, 1000, 1);
        var raster = rasterMap.get(Pollutant.NOx);

        // don't really know how to write assertions for this. So just see if things are blurred
        // if we reach here, it means nothing has crashed, all the logic is tested elsewhere
        raster.forEachIndex((xi, yi, value) -> assertTrue(biggestExpectedValue >= value));
    }
}
