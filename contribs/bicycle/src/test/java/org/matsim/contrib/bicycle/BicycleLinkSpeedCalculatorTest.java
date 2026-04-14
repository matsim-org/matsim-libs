package org.matsim.contrib.bicycle;

import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.mobsim.qsim.qnetsimengine.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BicycleLinkSpeedCalculatorTest {
    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();
    private static final double MAX_BICYCLE_SPEED = 15;

    private final Config config = ConfigUtils.createConfig();
    private BicycleConfigGroup bicycleConfigGroup;
    private final Network unusedNetwork = NetworkUtils.createNetwork();
	private BicycleLinkSpeedCalculator calculator;


    @BeforeEach
    public void before(TestInfo testInfo) {
	    bicycleConfigGroup = ConfigUtils.addOrGetModule( config, BicycleConfigGroup.class );
	    bicycleConfigGroup.setBicycleMode("bike" );
	    config.qsim().setVehiclesSource( QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData );
	    config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink );
		String testName = testInfo.getTestMethod().map(Method::getName).orElse("unknownTest");
		config.controller().setOutputDirectory("test/output/"+testName);
	    config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
	    Scenario scenario = ScenarioUtils.createScenario(config) ;
	    scenario.getVehicles().addModeVehicleType( "bike" ).setMaximumVelocity( 25./3.6 );
	    Controler controller = new Controler( scenario ) ;
	    controller.addOverridingModule(new BicycleModule());
	    calculator = controller.getInjector().getInstance(BicycleLinkSpeedCalculator.class);

	}


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


        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(vehicle.getMaximumVelocity(), speed, 0.0);
    }

	@Test
	void getMaximumVelocity_bike_fasterThanFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 2, 1));

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

	@Test
	void getMaximumVelocityForLink_bikeIsNull() {

		Link link = createLinkWithNoGradientAndNoSpecialSurface();

		assertThrows( NullPointerException.class,
			() -> calculator.getMaximumVelocityForLink(link, null),
			"if the vehicle is null, we expect an exception"
		);


//        assertEquals(configGroup.getMaxBicycleSpeedForRouting(), speed, 0.001);
	}

	@Test
	void getMaximumVelocityForLink_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);


        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithGradient, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

	@Test
	void getMaximumVelocityForLink_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithReducedSpeed, vehicle);

        assertTrue(comparisonSpeed > gradientSpeed);
    }

	@Test
	void getMaximumVelocityForLink_noSpeedFactor() {

        var link = createLinkWithNoGradientAndNoSpecialSurface();
        var vehicle = createVehicle(link.getFreespeed() + 1, 1);
        var speed = calculator.getMaximumVelocityForLink(link, vehicle);

        assertEquals(link.getFreespeed(), speed, 0.001);
    }

	@Test
	void getMaximumVelocityForLink_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);

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


        double comparisonSpeed = calculator.getMaximumVelocityForLink(linkForComparison, vehicle);
        double gradientSpeed = calculator.getMaximumVelocityForLink(linkWithCobbleStone, vehicle);

        assertEquals(comparisonSpeed, gradientSpeed, 0.0);
    }
}
