package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FastEmissionGridAnalyzerTest {

    @Test
    public void rasterNework_singelLink() {

        var network = NetworkUtils.createNetwork();
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(100, 0));
        var link = network.getFactory().createLink(Id.createLinkId("link"), node1, node2);
        network.addNode(node1);
        network.addNode(node2);
        network.addLink(link);

        var emissions = Map.of(link.getId(), 20.);

        var raster = FastEmissionGridAnalyzer.rasterNetwork(network, emissions, 10);

        assertEquals(11, raster.getXLength());
        assertEquals(1, raster.getYLength());
    }

    @Test
    public void rasterNetwork_twoLinks() {

        var network = NetworkUtils.createNetwork();
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(100, 0));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(0, 100));
        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node1, node3);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addLink(link1);
        network.addLink(link2);

        var emissions = Map.of(link1.getId(), 20., link2.getId(), 10.);

        var raster = FastEmissionGridAnalyzer.rasterNetwork(network, emissions, 10);

        assertEquals(11, raster.getXLength());
        assertEquals(11, raster.getYLength());
    }

    private static Network createRandomNetwork(int numberOfLinks, double maxX, double maxY) {

        Network network = NetworkUtils.createNetwork();

        for (long i = 0; i < numberOfLinks; i++) {

            Link link = createRandomLink(network.getFactory(), maxX, maxY);
            network.addNode(link.getFromNode());
            network.addNode(link.getToNode());
            network.addLink(link);
        }
        return network;
    }

    private static Link createRandomLink(NetworkFactory factory, double maxX, double maxY) {
        Node fromNode = createRandomNode(factory, maxX, maxY);
        Node toNode = createRandomNode(factory, maxX, maxY);
        return factory.createLink(Id.createLinkId(UUID.randomUUID().toString()), fromNode, toNode);
    }

    private static Node createRandomNode(NetworkFactory factory, double maxX, double maxY) {
        Coord coord = new Coord(getRandomValue(maxX), getRandomValue(maxY));
        return factory.createNode(Id.createNodeId(UUID.randomUUID().toString()), coord);
    }

    private static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }

    private static Map<Id<Link>, Double> createEmissions(Network network, double emissionValuePerLink) {

        Map<Id<Link>, Double> result = new HashMap<>();
        for (Link value : network.getLinks().values()) {

            result.put(value.getId(), emissionValuePerLink);
        }

        return result;
    }

    @Test
    public void smooth_singleLink() {

        var network = NetworkUtils.createNetwork();
        var node1 = network.getFactory().createNode(Id.createNodeId("node1"), new Coord(0, 0));
        var node2 = network.getFactory().createNode(Id.createNodeId("node2"), new Coord(100, 0));
        var node3 = network.getFactory().createNode(Id.createNodeId("node3"), new Coord(0, 100));
        var link1 = network.getFactory().createLink(Id.createLinkId("link1"), node1, node2);
        var link2 = network.getFactory().createLink(Id.createLinkId("link2"), node1, node3);
        network.addNode(node1);
        network.addNode(node2);
        network.addNode(node3);
        network.addLink(link1);
        network.addLink(link2);

        var emissions = Map.of(link1.getId(), 20., link2.getId(), 10.);

        var smoothedRaster = FastEmissionGridAnalyzer.calculate(network, emissions, 10, 3);

        assertNotNull(smoothedRaster);

    }

    @Test
    public void benchmark() {

        var network = createRandomNetwork(10000, 100000, 100000);
        var emissions = createEmissions(network, 20);

        var smoothedRaster = FastEmissionGridAnalyzer.calculate(network, emissions, 100, 3);

        assertNotNull(smoothedRaster);


    }
}