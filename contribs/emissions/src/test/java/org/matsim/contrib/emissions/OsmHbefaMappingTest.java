package org.matsim.contrib.emissions;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.*;

public class OsmHbefaMappingTest {

    @Test
    public void testRegularMapping() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("primary", 70 / 3.6);

        var hbefaType = mapping.determineHebfaType(link);

        assertEquals("URB/Trunk-City/70", hbefaType);
    }

    @Test(expected = RuntimeException.class)
    public void testUnknownType() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("unknown-tag", 100 / 3.6);

        mapping.determineHebfaType(link);

        fail("Expected Runtime Exception.");
    }

    @Test
    public void testFastMotorway() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("motorway", 100 / 3.6);

        var hbefaType = mapping.determineHebfaType(link);

        assertEquals("URB/MW-Nat./100", hbefaType);
    }

    @Test
    public void testUnclassified() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("unclassified", 50 / 3.6);

        var hbefaType = mapping.determineHebfaType(link);

        assertEquals("URB/Access/50", hbefaType);
    }

    @Test
    public void testNoHighwayType() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink(" ", 60 / 3.6);

        var hbefaType = mapping.determineHebfaType(link);

        assertEquals("URB/Trunk-City/60", hbefaType);
    }

    @Test
    public void testNoAllowedSpeedTag() {

        var mapping = OsmHbefaMapping.build();
        var link = getTestLink("residential", 40 / 3.6);
        link.getAttributes().removeAttribute(NetworkUtils.ALLOWED_SPEED);

        var hbefaType = mapping.determineHebfaType(link);

        assertEquals("URB/Access/40", hbefaType);
    }

    private static Link getTestLink(String osmRoadType, double allowedSpeed) {

        var network = NetworkUtils.createNetwork();
        var from = network.getFactory().createNode(Id.createNodeId("from"), new Coord(0, 0));
        var to = network.getFactory().createNode(Id.createNodeId("to"), new Coord(0, 1000));
        var link = network.getFactory().createLink(Id.createLinkId("link"), from, to);
        link.setFreespeed(allowedSpeed);
        link.setCapacity(1000);
        link.setNumberOfLanes(1);
        link.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, allowedSpeed);
        link.getAttributes().putAttribute(NetworkUtils.TYPE, osmRoadType);

        return link;
    }
}