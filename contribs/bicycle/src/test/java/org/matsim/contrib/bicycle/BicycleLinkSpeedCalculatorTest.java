package org.matsim.contrib.bicycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BicycleLinkSpeedCalculatorTest {
    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();
    private static final double MAX_BICYCLE_SPEED = 15;

    private final Config config = ConfigUtils.createConfig();
    private BicycleConfigGroup configGroup;
    private final Network unusedNetwork = NetworkUtils.createNetwork();

    @BeforeEach
    public void before() {
        configGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
        configGroup.setMaxBicycleSpeedForRouting(MAX_BICYCLE_SPEED);
        configGroup.setBicycleMode("bike");
    }

    // The  more general parts of the test (testing different car behavior) were moved to SpeedCalculatorTest in the core, because there it was
    // possible to make something package-accessible.  kai, jun'23

    private static Vehicle createVehicle(double maxVelocity, long id) {
        VehicleType type = VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class));
        type.setMaximumVelocity(maxVelocity);
        type.setNetworkMode( "bike" );
        return VehicleUtils.createVehicle(Id.createVehicleId(id), type);
    }

	@Test
	void getMaximumVelocity_bike() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 0.5, 1)); // higher speed than the default
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(vehicle.getMaximumVelocity(), speed, 0.0);
    }

	@Test
	void getMaximumVelocity_bike_fasterThanFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 2, 1));
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

	@Test
	void getMaximumVelocityForLink_bikeIsNull() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double speed = calculator.getMaximumVelocityForLink(link, null);

        assertEquals(configGroup.getMaxBicycleSpeedForRouting(), speed, 0.001);
    }

	@Test
	void getMaximumVelocityForLink_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

		BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithGradient, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

	@Test
	void getMaximumVelocityForLink_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithReducedSpeed, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

	@Test
	void getMaximumVelocityForLink_noSpeedFactor() {

        var link = createLinkWithNoGradientAndNoSpecialSurface();
        var vehicle = createVehicle(link.getFreespeed() + 1, 1);
        var calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        var speed = calculator.getMaximumVelocityForLink(link, vehicle);

        assertEquals(link.getFreespeed(), speed, 0.001);
    }

	@Test
	void getMaximumVelocityForLink_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);
        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

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
	void getMaximumVelocityForLink_withCycleWay() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "some-surface", BicycleUtils.CYCLEWAY, 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);

        BicycleLinkSpeedCalculatorDefaultImpl calculator = new BicycleLinkSpeedCalculatorDefaultImpl(config);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone, vehicle);

        assertEquals(comparisonSpeed, gradientSpeed, 0.0);
    }
}
