package org.matsim.contrib.emissions.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.UUID;

public class EmissionGridAnalyzerTest {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

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

    static double getRandomValue(double upperBounds) {
        return Math.random() * upperBounds;
    }

    @Test
    public void process() {

        String workingDirectory = testUtils.getClassInputDirectory();
        int numberOfLinks = 1000;
        Network network = createRandomNetwork(numberOfLinks, 1000, 1000);

        /*EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer(10, 10, SpatialInterpolation.GridType.Square,
                network, )*/


    }
}
