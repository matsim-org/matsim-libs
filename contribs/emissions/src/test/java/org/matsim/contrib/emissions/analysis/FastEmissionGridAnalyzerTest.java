package org.matsim.contrib.emissions.analysis;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FastEmissionGridAnalyzerTest {

    @Test
    public void singleLink() {

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
    public void twoLinks() {

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
}