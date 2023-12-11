/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.core.network.algorithms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Created by amit on 31.10.17.
 *
 * See https://matsim.atlassian.net/browse/MATSIM-755.
 */

public class NetworkSimplifierPass2WayTest {

	@Test
	void testSimplifying(){
        List<Network> networks = buildNetworks();

        int counter = 0;
        for (Network network : networks) {
            System.out.println("Running simplifier on network "+counter);
            assertEquals(10, network.getLinks().size(), "Wrong number of links");

            NetworkSimplifier networkSimplifier = new NetworkSimplifier();
            networkSimplifier.setMergeLinkStats(false);
            networkSimplifier.run(network);

            network.getLinks().values().stream().forEach(l ->System.out.println(l.toString()));

            assertEquals(4, network.getLinks().size(), "Wrong number of links");
            assertNotNull(network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")), "Expected link not found.");

            assertNotNull(network.getLinks().get(Id.createLinkId("CB")), "Expected link not found.");
            assertNotNull(network.getLinks().get(Id.createLinkId("BA")), "Expected link not found.");

            assertNotNull(network.getLinks().get(Id.createLinkId("FE-ED-DC")), "Expected link not found.");

        }

//        if (randomSequenceOfLinksToNetwork) {
//            assertEquals("Wrong number of links", 4, network.getLinks().size());
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")));
//
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CB")));
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("BA")));
//
//            //following is fixed.
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("FE-ED")));
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DC"))); // this is undesired link.
//        } else {
//            assertEquals("Wrong number of links", 4, network.getLinks().size());
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("AB-BC-CD-DE-EF")));
//
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("CB")));
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("BA")));
//
//            assertNotNull("Expected link not found.", network.getLinks().get(Id.createLinkId("FE-ED-DC")));
//        }
    }

    /**
     * Builds a test network like the following diagram.
     *
     * A<===>B<===>C<===>D<===>E<===>F
     *
     * Each link has one lane, 10 m, A to F is one direction and F to A is other.
     * The capacity of each link is same except CB.
     *
     * @return
     */
    private List<Network> buildNetworks(){

        Node a = NetworkUtils.createNode(Id.createNodeId("a"),CoordUtils.createCoord(0.0,  0.0));
        Node b = NetworkUtils.createNode(Id.createNodeId("b"), CoordUtils.createCoord(10.0,  0.0));
        Node c = NetworkUtils.createNode(Id.createNodeId("c"), CoordUtils.createCoord(20.0,  0.0));
        Node d = NetworkUtils.createNode(Id.createNodeId("d"), CoordUtils.createCoord(30.0,  0.0));
        Node e = NetworkUtils.createNode(Id.createNodeId("e"), CoordUtils.createCoord(40.0,  0.0));
        Node f = NetworkUtils.createNode(Id.createNodeId("f"), CoordUtils.createCoord(50.0,  0.0));

        List<Network> networks = new ArrayList<>();
        {
            List<Node> nodes = Arrays.asList(a, b, c, d, e, f);

            for (int i = 0; i< nodes.size(); i++){
                System.out.println("Nodes ordering index "+ i);
                networks.add(reshuffleNodesAndReturnNetwork(new ArrayList<>(nodes)));
            }
        }

        for (Network network : networks) {
            NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), network.getNodes().get(Id.createNodeId("a")), network.getNodes().get(Id.createNodeId("b")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), network.getNodes().get(Id.createNodeId("b")), network.getNodes().get(Id.createNodeId("c")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("CD"), network.getNodes().get(Id.createNodeId("c")), network.getNodes().get(Id.createNodeId("d")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("DE"), network.getNodes().get(Id.createNodeId("d")), network.getNodes().get(Id.createNodeId("e")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("EF"), network.getNodes().get(Id.createNodeId("e")), network.getNodes().get(Id.createNodeId("f")), 10.0, 60.0/3.6, 1000.0, 1);

            NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), network.getNodes().get(Id.createNodeId("b")), network.getNodes().get(Id.createNodeId("a")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("CB"), network.getNodes().get(Id.createNodeId("c")), network.getNodes().get(Id.createNodeId("b")), 10.0, 60.0/3.6, 2000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("DC"), network.getNodes().get(Id.createNodeId("d")), network.getNodes().get(Id.createNodeId("c")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("ED"), network.getNodes().get(Id.createNodeId("e")), network.getNodes().get(Id.createNodeId("d")), 10.0, 60.0/3.6, 1000.0, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("FE"), network.getNodes().get(Id.createNodeId("f")), network.getNodes().get(Id.createNodeId("e")), 10.0, 60.0/3.6, 1000.0, 1);
        }
        return networks;
    }

    private Network reshuffleNodesAndReturnNetwork(List<Node> nodes) {
        Network network = NetworkUtils.createNetwork();
        Collections.shuffle(nodes);
        for (Node n : nodes) {
            System.out.println("Adding node "+ n.getId());
            network.addNode(NetworkUtils.createNode(n.getId(), n.getCoord()));
        }
        return network;
    }


}
