/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *  * ***********************************************************************
 */

package playground.pieter.singapore.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by fouriep on 5/14/15.
 */
public class NetworkAngleUtilsTest {

    private final static Network network = (Network) NetworkUtils.createNetwork();

    //static block
    {
        if (network.getNodes().size() == 0) {

//            8   1==== 2
//             \\ ^  // ||
//              \\| //  ||
//        7===6===0---->3
//                ^     |
//                |     v
//                5<----4

            Node n0 = NetworkUtils.createAndAddNode(network, Id.createNodeId(0), new Coord((double) 0, (double) 0));
            Node n1 = NetworkUtils.createAndAddNode(network, Id.createNodeId(1), new Coord((double) 0, (double) 1));
            Node n2 = NetworkUtils.createAndAddNode(network, Id.createNodeId(2), new Coord((double) 1, (double) 1));
            Node n3 = NetworkUtils.createAndAddNode(network, Id.createNodeId(3), new Coord((double) 1, (double) 0));
            final double y1 = -1;
            Node n4 = NetworkUtils.createAndAddNode(network, Id.createNodeId(4), new Coord((double) 1, y1));
            final double y = -1;
            Node n5 = NetworkUtils.createAndAddNode(network, Id.createNodeId(5), new Coord((double) 0, y));
            final double x2 = -1;
            Node n6 = NetworkUtils.createAndAddNode(network, Id.createNodeId(6), new Coord(x2, (double) 0));
            final double x1 = -2;
            Node n7 = NetworkUtils.createAndAddNode(network, Id.createNodeId(7), new Coord(x1, (double) 0));
            final double x = -1;
            Node n8 = NetworkUtils.createAndAddNode(network, Id.createNodeId(8), new Coord(x, (double) 1));
		final Node fromNode = n0;
		final Node toNode = n1;

            //organize creation by node outlinks
            NetworkUtils.createAndAddLink(network,Id.createLinkId(1), fromNode, toNode, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode1 = n0;
		final Node toNode1 = n2;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(2), fromNode1, toNode1, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode2 = n0;
		final Node toNode2 = n3;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(3), fromNode2, toNode2, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode3 = n0;
		final Node toNode3 = n6;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(6), fromNode3, toNode3, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode4 = n0;
		final Node toNode4 = n8;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(8), fromNode4, toNode4, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode5 = n1;
		final Node toNode5 = n2;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(12), fromNode5, toNode5, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode6 = n2;
		final Node toNode6 = n0;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(20), fromNode6, toNode6, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode7 = n2;
		final Node toNode7 = n1;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(21), fromNode7, toNode7, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode8 = n2;
		final Node toNode8 = n3;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(23), fromNode8, toNode8, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode9 = n3;
		final Node toNode9 = n2;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(32), fromNode9, toNode9, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode10 = n3;
		final Node toNode10 = n4;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(34), fromNode10, toNode10, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode11 = n4;
		final Node toNode11 = n5;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(45), fromNode11, toNode11, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode12 = n5;
		final Node toNode12 = n0;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(50), fromNode12, toNode12, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode13 = n6;
		final Node toNode13 = n0;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(60), fromNode13, toNode13, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode14 = n6;
		final Node toNode14 = n7;
            NetworkUtils.createAndAddLink(network,Id.createLinkId(67), fromNode14, toNode14, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode15 = n7;
		final Node toNode15 = n6;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(76), fromNode15, toNode15, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Node fromNode16 = n8;
		final Node toNode16 = n0;

            NetworkUtils.createAndAddLink(network,Id.createLinkId(80), fromNode16, toNode16, (double) 1, (double) 1, (double) 1, (double) 1 );
        }

    }

    @Test
    public void testGetInterSections() throws Exception {
        Set<Id<Node>> interSections = NetworkAngleUtils.getInterSections(network);
        Set<Id<Node>> interSectionsExpected = new HashSet<>();
        interSectionsExpected.add(Id.createNodeId(0));
        interSectionsExpected.add(Id.createNodeId(1));
        interSectionsExpected.add(Id.createNodeId(2));
        interSectionsExpected.add(Id.createNodeId(3));
        Assert.assertTrue(interSections.containsAll(interSectionsExpected));
        Assert.assertFalse(interSections.contains(Id.createNodeId(4)));
        Assert.assertFalse(interSections.contains(Id.createNodeId(5)));
        Assert.assertFalse(interSections.contains(Id.createNodeId(6)));
        Assert.assertFalse(interSections.contains(Id.createNodeId(7)));
        Assert.assertFalse(interSections.contains(Id.createNodeId(8)));
    }

    @Test
    public void testGetAngleBetweenLinks() throws Exception {
        Link l1 = network.getLinks().get(Id.createLinkId(1));
        Link l50 = network.getLinks().get(Id.createLinkId(50));
        Link l2 = network.getLinks().get(Id.createLinkId(2));
        Link l3 = network.getLinks().get(Id.createLinkId(3));
        Link l20 = network.getLinks().get(Id.createLinkId(20));
        Link l60 = network.getLinks().get(Id.createLinkId(60));
        Link l8 = network.getLinks().get(Id.createLinkId(8));
        Assert.assertEquals(0.0, NetworkAngleUtils.getAngleBetweenLinks(l50, l1), MatsimTestUtils.EPSILON);
        Assert.assertEquals(Math.toRadians(-135.0), NetworkAngleUtils.getAngleBetweenLinks(l20, l1), MatsimTestUtils.EPSILON); //sharp right
        Assert.assertEquals(Math.toRadians(90.0), NetworkAngleUtils.getAngleBetweenLinks(l60, l1), MatsimTestUtils.EPSILON);
        Assert.assertEquals(Math.toRadians(90.0), NetworkAngleUtils.getAngleBetweenLinks(l60, l50), MatsimTestUtils.EPSILON); //
        Assert.assertEquals(Math.toRadians(45.0), NetworkAngleUtils.getAngleBetweenLinks(l50, l8), MatsimTestUtils.EPSILON);
        Assert.assertEquals(Math.toRadians(-45.0), NetworkAngleUtils.getAngleBetweenLinks(l50, l2), MatsimTestUtils.EPSILON);
        Assert.assertEquals(Math.toRadians(-90.0), NetworkAngleUtils.getAngleBetweenLinks(l50, l3), MatsimTestUtils.EPSILON);
    }

    @Test
    public void testIsIntersection() throws Exception {
        Assert.assertTrue(NetworkAngleUtils.isIntersection(network.getNodes().get(Id.createNodeId(0))));
        Assert.assertFalse(NetworkAngleUtils.isIntersection(network.getNodes().get(Id.createNodeId(4))));
        Assert.assertFalse(NetworkAngleUtils.isIntersection(network.getNodes().get(Id.createNodeId(6))));
        Assert.assertTrue(NetworkAngleUtils.isIntersection(network.getNodes().get(Id.createNodeId(1))));
    }
}