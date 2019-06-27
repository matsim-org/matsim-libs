package org.matsim.contrib.bicycle;

import org.junit.BeforeClass;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BicycleLinkSpeedCalculatorTest {

    private static final BicycleConfigGroup configGroup = new BicycleConfigGroup();
    private static final double maxBicycleSpeed = 15;
    private static Network unusedNetwork = NetworkUtils.createNetwork();

    @BeforeClass
    public static void before() {

        configGroup.setMaxBicycleSpeedForRouting(maxBicycleSpeed);
        configGroup.setBicycleMode("bike");
    }

    @Test
    public void getMaximumVelocity_noBike() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = new VehicleTypeImpl(Id.create("no-bike", VehicleType.class));
        type.setMaximumVelocity(link.getFreespeed() / 2); // less than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(new VehicleImpl(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(type.getMaximumVelocity(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_noBike_vehicleFasterThanFreespeed() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = new VehicleTypeImpl(Id.create("no-bike", VehicleType.class));
        type.setMaximumVelocity(link.getFreespeed() * 10); // more than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(new VehicleImpl(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_bike() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = new VehicleTypeImpl(Id.create("bike", VehicleType.class));
        type.setMaximumVelocity(1000); // more than in the config group
        QVehicle vehicle = new QVehicleImpl(new VehicleImpl(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(configGroup.getMaxBicycleSpeedForRouting(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_bike_fasterThanFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        link.setFreespeed(configGroup.getMaxBicycleSpeedForRouting() * 0.5); // the freespeed should be less than the assumed bike speed
        VehicleType type = new VehicleTypeImpl(Id.create("bike", VehicleType.class));
        type.setMaximumVelocity(link.getFreespeed() * 2);
        QVehicle vehicle = new QVehicleImpl(new VehicleImpl(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocityForLink_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);

		BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

		double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison);
		double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithGradient);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);

		BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

		double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison);
		double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithReducedSpeed);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

		BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

		double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison);
		double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_withCycleWay() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "some-surface", BicycleUtils.CYCLEWAY, 1.0);

		BicycleLinkSpeedCalculator calculator = new BicycleLinkSpeedCalculator(configGroup);

		double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison);
		double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone);

        assertEquals(comparisonSpeed, gradientSpeed, 0.0);
    }

    private Link createLinkWithNoGradientAndNoSpecialSurface() {

        Coord from = new Coord(0, 0, 0);
        Coord to = new Coord(100, 0, 0);
        double length = 100;
        Node fromNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), from);
        Node toNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), to);
        Link link = NetworkUtils.createAndAddLink(unusedNetwork, Id.createLinkId(UUID.randomUUID().toString()),
                fromNode, toNode, length, 1000, 1000, 1);
        link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0);
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
        link.getAttributes().putAttribute(BicycleUtils.SURFACE, surfaceType);
        link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, speedFactor);
        link.getAttributes().putAttribute(BicycleUtils.WAY_TYPE, wayType);
        return link;
    }
}
