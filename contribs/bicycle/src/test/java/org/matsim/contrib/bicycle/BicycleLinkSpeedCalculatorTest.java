package org.matsim.contrib.bicycle;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BicycleLinkSpeedCalculatorTest {

    private static final double MAX_BICYCLE_SPEED = 15;

    private final BicycleConfigGroup configGroup = new BicycleConfigGroup();
    private final Network unusedNetwork = NetworkUtils.createNetwork();

    @Before
    public void before() {
        configGroup.setMaxBicycleSpeedForRouting(MAX_BICYCLE_SPEED);
        configGroup.setBicycleMode("bike");
    }

    @Test
    public void getMaximumVelocity_noBike() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ) );
        type.setMaximumVelocity(link.getFreespeed() / 2); // less than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(type.getMaximumVelocity(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_noBike_vehicleFasterThanFreespeed() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ) );
        type.setMaximumVelocity(link.getFreespeed() * 10); // more than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

    private static Vehicle createVehicle(double maxVelocity, long id) {
        VehicleType type = VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class));
        type.setMaximumVelocity(maxVelocity);
        return VehicleUtils.createVehicle(Id.createVehicleId(id), type);
    }

    @Test
    public void getMaximumVelocity_bike() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 0.5, 1)); // higher speed than the default
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(vehicle.getMaximumVelocity(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_bike_fasterThanFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 2, 1));
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocityForLink_bikeIsNull() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double speed = calculator.getMaximumVelocityForLink(link, null);

        assertEquals(configGroup.getMaxBicycleSpeedForRouting(), speed, 0.001);
    }

    @Test
    public void getMaximumVelocityForLink_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

		BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithGradient, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithReducedSpeed, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_noSpeedFactor() {

        var link = createLinkWithNoGradientAndNoSpecialSurface();
        var vehicle = createVehicle(link.getFreespeed() + 1, 1);
        var calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        var speed = calculator.getMaximumVelocityForLink(link, vehicle);

        assertEquals(link.getFreespeed(), speed, 0.001);
    }

    @Test
    public void getMaximumVelocityForLink_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    private Link createLinkWithNoGradientAndNoSpecialSurface() {

        Coord from = new Coord(0, 0, 0);
        Coord to = new Coord(100, 0, 0);
        double length = 100;
        Node fromNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), from);
        Node toNode = NetworkUtils.createAndAddNode(unusedNetwork, Id.createNodeId(UUID.randomUUID().toString()), to);
        return NetworkUtils.createAndAddLink(unusedNetwork, Id.createLinkId(UUID.randomUUID().toString()),
                fromNode, toNode, length, 1000, 1000, 1);
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

    @Test
    public void getMaximumVelocityForLink_withCycleWay() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "some-surface", BicycleUtils.CYCLEWAY, 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);

        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(configGroup);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone, vehicle);

        assertEquals(comparisonSpeed, gradientSpeed, 0.0);
    }
}
