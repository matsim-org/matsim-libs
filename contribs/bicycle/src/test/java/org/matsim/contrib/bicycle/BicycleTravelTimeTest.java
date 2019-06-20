package org.matsim.contrib.bicycle;

import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BicycleTravelTimeTest {

    private static final BicycleConfigGroup configGroup = new BicycleConfigGroup();
    private static final double maxBicycleSpeed = 15;
    private static Network unusedNetwork = NetworkUtils.createNetwork();

    @BeforeClass
    public static void before() {
        configGroup.setMaxBicycleSpeedForRouting(maxBicycleSpeed);
    }

    @Test
    public void getLinkTravelTime_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);

        BicycleTravelTime travelTime = new BicycleTravelTime(configGroup);

        double comparisonTime = travelTime.getLinkTravelTime(linkForComparison, 1, null, null);
        double gradientTime = travelTime.getLinkTravelTime(linkWithGradient, 1, null, null);

        assertTrue(comparisonTime < gradientTime);
    }

    @Test
    public void getLinkTravelTime_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);

        BicycleTravelTime travelTime = new BicycleTravelTime(configGroup);

        double comparisonTime = travelTime.getLinkTravelTime(linkForComparison, 1, null, null);
        double reducedTime = travelTime.getLinkTravelTime(linkWithReducedSpeed, 1, null, null);

        assertTrue(comparisonTime < reducedTime);
    }

    @Test
    public void getLinkTravelTime_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

        BicycleTravelTime travelTime = new BicycleTravelTime(configGroup);

        double comparisonTime = travelTime.getLinkTravelTime(linkForComparison, 1, null, null);
        double cobblestoneTime = travelTime.getLinkTravelTime(linkWithCobbleStone, 1, null, null);

        assertTrue(comparisonTime < cobblestoneTime);
    }

    @Test
    public void getLinkTravelTime_withCycleWay() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "some-surface", BicycleLabels.CYCLEWAY, 1.0);

        BicycleTravelTime travelTime = new BicycleTravelTime(configGroup);

        double comparisonTime = travelTime.getLinkTravelTime(linkForComparison, 1, null, null);
        double cobblestoneTime = travelTime.getLinkTravelTime(linkWithCobbleStone, 1, null, null);

        assertEquals(comparisonTime, cobblestoneTime, 0.0);
    }

    private Link createLinkWithNoGradientAndNoSpecialSurface() {

        Coord from = new Coord(0, 0, 0);
        Coord to = new Coord(100, 0, 0);
        double length = 100;
        Node fromNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), from);
        Node toNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), to);
        Link link = NetworkUtils.createAndAddLink(unusedNetwork, Id.createLinkId(UUID.randomUUID().toString()),
                fromNode, toNode, length, 1000, 1000, 1);
        link.getAttributes().putAttribute(BicycleLabels.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0);
        return link;
    }

    private Link createLink(double elevation, String surfaceType, String wayType, double speedFactor) {

        Coord from = new Coord(0, 0, 0);
        Coord to = new Coord(100, 0, elevation); // steep
        double length = 100;

        Node fromNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), from);
        Node toNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), to);
        Link link = NetworkUtils.createAndAddLink(unusedNetwork, Id.createLinkId(UUID.randomUUID().toString()),
                fromNode, toNode, length, 1000, 1000, 1);
        link.getAttributes().putAttribute(BicycleLabels.SURFACE, surfaceType);
        link.getAttributes().putAttribute(BicycleLabels.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, speedFactor);
        link.getAttributes().putAttribute(BicycleLabels.WAY_TYPE, wayType);
        return link;
    }
}
