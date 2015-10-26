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
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.MatsimTestUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by fouriep on 5/14/15.
 */
public class NetworkAngleUtilsTest {

    private final static NetworkImpl network = (NetworkImpl) NetworkUtils.createNetwork();

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

            Node n0 = network.createAndAddNode(Id.createNodeId(0), new Coord((double) 0, (double) 0));
            Node n1 = network.createAndAddNode(Id.createNodeId(1), new Coord((double) 0, (double) 1));
            Node n2 = network.createAndAddNode(Id.createNodeId(2), new Coord((double) 1, (double) 1));
            Node n3 = network.createAndAddNode(Id.createNodeId(3), new Coord((double) 1, (double) 0));
            final double y1 = -1;
            Node n4 = network.createAndAddNode(Id.createNodeId(4), new Coord((double) 1, y1));
            final double y = -1;
            Node n5 = network.createAndAddNode(Id.createNodeId(5), new Coord((double) 0, y));
            final double x2 = -1;
            Node n6 = network.createAndAddNode(Id.createNodeId(6), new Coord(x2, (double) 0));
            final double x1 = -2;
            Node n7 = network.createAndAddNode(Id.createNodeId(7), new Coord(x1, (double) 0));
            final double x = -1;
            Node n8 = network.createAndAddNode(Id.createNodeId(8), new Coord(x, (double) 1));

            //organize creation by node outlinks
            network.createAndAddLink(Id.createLinkId(1), n0, n1, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(2), n0, n2, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(3), n0, n3, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(6), n0, n6, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(8), n0, n8, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(12), n1, n2, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(20), n2, n0, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(21), n2, n1, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(23), n2, n3, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(32), n3, n2, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(34), n3, n4, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(45), n4, n5, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(50), n5, n0, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(60), n6, n0, 1, 1, 1, 1);
            network.createAndAddLink(Id.createLinkId(67), n6, n7, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(76), n7, n6, 1, 1, 1, 1);

            network.createAndAddLink(Id.createLinkId(80), n8, n0, 1, 1, 1, 1);
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