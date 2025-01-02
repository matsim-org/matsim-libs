package org.matsim.dsim;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

class NetworkDecompositionTest {

    @Test
    public void testSimpleNetwork() {

        Network network = NetworkUtils.createNetwork();
        Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig(), network);

        var n1 = network.getFactory().createNode(Id.createNodeId("n1"), new Coord(-100, -100));
        var n2 = network.getFactory().createNode(Id.createNodeId("n2"), new Coord(-100, 100));
        var n3 = network.getFactory().createNode(Id.createNodeId("n3"), new Coord(100, 100));
        var n4 = network.getFactory().createNode(Id.createNodeId("n4"), new Coord(100, -100));

        var l1 = network.getFactory().createLink(Id.createLinkId("l1"), n1, n2);
        var l2 = network.getFactory().createLink(Id.createLinkId("l2"), n2, n3);
        var l3 = network.getFactory().createLink(Id.createLinkId("l3"), n3, n4);
        var l4 = network.getFactory().createLink(Id.createLinkId("l4"), n4, n1);

        network.addNode(n1);
        network.addNode(n2);
        network.addNode(n3);
        network.addNode(n4);

        network.addLink(l1);
        network.addLink(l2);
        network.addLink(l3);
        network.addLink(l4);

        NetworkDecomposition.bisection(network, population, 4);

        System.out.println(network);

        n1 = network.getNodes().get(Id.createNodeId("n1"));
        assertEquals(0, (int) n1.getAttributes().getAttribute(PARTITION_ATTR_KEY));
        l4 = network.getLinks().get(Id.createLinkId("l4"));
        assertEquals(0, (int) l4.getAttributes().getAttribute(PARTITION_ATTR_KEY));

        n2 = network.getNodes().get(Id.createNodeId("n2"));
        assertEquals(1, (int) n2.getAttributes().getAttribute(PARTITION_ATTR_KEY));
        l1 = network.getLinks().get(Id.createLinkId("l1"));
        assertEquals(1, (int) l1.getAttributes().getAttribute(PARTITION_ATTR_KEY));

        n3 = network.getNodes().get(Id.createNodeId("n3"));
        assertEquals(2, (int) n3.getAttributes().getAttribute(PARTITION_ATTR_KEY));
        l2 = network.getLinks().get(Id.createLinkId("l2"));
        assertEquals(2, (int) l2.getAttributes().getAttribute(PARTITION_ATTR_KEY));

        n4 = network.getNodes().get(Id.createNodeId("n4"));
        assertEquals(3, (int) n4.getAttributes().getAttribute(PARTITION_ATTR_KEY));
        l3 = network.getLinks().get(Id.createLinkId("l3"));
        assertEquals(3, (int) l3.getAttributes().getAttribute(PARTITION_ATTR_KEY));
    }
}
