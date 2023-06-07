package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpeedCalculatorTest{
    // This originally comes from the bicycle contrib.  One now needs access to AbstractQLink to properly test this functionality. In contrast, the
    // specific bicycle material should not be necessary. kai, jun'23

    @Rule public MatsimTestUtils utils = new MatsimTestUtils();
    private final Config config = ConfigUtils.createConfig();
    private final Network unusedNetwork = NetworkUtils.createNetwork();

    @Test public void limitedByVehicleSpeed() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ) );
        type.setMaximumVelocity(link.getFreespeed() / 2); // less than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));

        SpecificLinkSpeedCalculator specificLinkSpeedCalculator = new SpecificLinkSpeedCalculator();
        {
            double speed = specificLinkSpeedCalculator.getMaximumVelocity( vehicle, link, 1 );
            assertTrue( Double.isNaN( speed ) );
            // (specific link speed calculator is not responsible)
        }
        {
            config.controler().setOutputDirectory( utils.getOutputDirectory() );
            Scenario scenario = ScenarioUtils.createScenario( config );
            AbstractQSimModule module = new AbstractQSimModule(){
                @Override public void configureQSim(){
                    bind( DefaultLinkSpeedCalculator.class );
                    bind( LinkSpeedCalculator.class ).to( SpecificLinkSpeedCalculator.class );
                }
            };
            {
                EventsManager eventsManager = EventsUtils.createEventsManager();
                QSim qsim = new QSimBuilder(config).useDefaults().addQSimModule( module )
//                                                    .configureComponents(components -> {
//                                                        components.activeMobsimEngines.add("MyCustomMobsimEngine");
//                                                    }) //
                                                    .build(scenario, eventsManager);
                var childInjector = qsim.getChildInjector();

                var nf = scenario.getNetwork().getFactory();
                Node fromNode = nf.createNode( Id.createNodeId( "fromNode" ), new Coord( 0.,0. ) );
                Node toNode = nf.createNode( Id.createNodeId( "toNode") , new Coord( 1000.,0 ) );
                Link dummyLink = nf.createLink( Id.createLinkId( "link" ), fromNode, toNode );
                var qNetworkFactory = childInjector.getInstance( QNetworkFactory.class );
                var qLink = (AbstractQLink) qNetworkFactory.createNetsimLink( dummyLink, qNetworkFactory.createNetsimNode( toNode ) );
                var freeSpeedOnLink = qLink.getInternalInterface().getMaximumVelocityFromLinkSpeedCalculator( vehicle, 1. );

                assertEquals( type.getMaximumVelocity(), freeSpeedOnLink, 0.0 );

            }
        }

    }

    @Test
    public void getMaximumVelocity_noBike_vehicleFasterThanFreespeed() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ) );
        type.setMaximumVelocity(link.getFreespeed() *10 ); // more than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));
        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();
        {
            double speed = calculator.getMaximumVelocity( vehicle, link, 1 );
            assertTrue( Double.isNaN( speed ) );
        }
        {
            config.controler().setOutputDirectory( utils.getOutputDirectory() );
            Scenario scenario = ScenarioUtils.createScenario( config );
            AbstractModule module = new AbstractModule( config ){
                @Override public void install(){
                    bind( DefaultLinkSpeedCalculator.class );
                    bind( LinkSpeedCalculator.class ).to( SpecificLinkSpeedCalculator.class );
                }
            };
            var injector = Injector.createMinimalMatsimInjector( config, scenario, module );
            {
                var linkSpeedCalculator = injector.getInstance( DefaultLinkSpeedCalculator.class );
                linkSpeedCalculator.addLinkSpeedCalculator( calculator );
                var speed = linkSpeedCalculator.getMaximumVelocity( vehicle, link, 1 );
                assertEquals( link.getFreespeed( 1 ), speed, 0.0 );
            }
            {
                var linkSpeedCalculator = injector.getInstance( LinkSpeedCalculator.class );
                var speed = linkSpeedCalculator.getMaximumVelocity( vehicle, link, 1 );
                assertEquals( link.getFreespeed( 1 ), speed, 0.0 );
            }
        }

    }

    private static Vehicle createVehicle(double maxVelocity, long id) {
        VehicleType type = VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class));
        type.setMaximumVelocity(maxVelocity);
        type.setNetworkMode( "bike" );
        return VehicleUtils.createVehicle(Id.createVehicleId(id), type);
    }

    @Test
    public void getMaximumVelocity_bike() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 0.5, 1)); // higher speed than the default
        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(vehicle.getMaximumVelocity(), speed, 0.0);
    }

    @Test
    public void getMaximumVelocity_bike_fasterThanFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        QVehicle vehicle = new QVehicleImpl(createVehicle(link.getFreespeed() * 2, 1));
        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();

        double speed = calculator.getMaximumVelocity(vehicle, link, 1);

        assertEquals(link.getFreespeed(), speed, 0.0);
    }

//    @Test
//    public void getMaximumVelocityForLink_bikeIsNull() {
//
//        Link link = createLinkWithNoGradientAndNoSpecialSurface();
//        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator(config);
//
//        double maxBicycleSpeed = ((Vehicle) null).getType().getMaximumVelocity();
//        double speed1 = maxBicycleSpeed;
//        double speed = Math.min( speed1, link.getFreespeed() );
//
//        assertEquals(configGroup.getMaxBicycleSpeedForRouting(), speed, 0.001);
//    }

    @Test
    public void getMaximumVelocityForLink_withGradient() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithGradient = createLink(100, "paved", "not-a-cycle-way", 1.0);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

		SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();

        double maxBicycleSpeed1 = vehicle.getType().getMaximumVelocity();
        double speed1 = maxBicycleSpeed1;
        double comparisonSpeed = Math.min( speed1, linkForComparison.getFreespeed() );

        double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
        double speed = maxBicycleSpeed;
        double gradientSpeed = Math.min( speed, linkWithGradient.getFreespeed() );

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_withReducedSpeedFactor() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithReducedSpeed = createLink(0, "paved", "not-a-cycle-way", 0.5);
        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed() * 0.5, 1);

        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();

        double maxBicycleSpeed1 = vehicle.getType().getMaximumVelocity();
        double speed1 = maxBicycleSpeed1;
        double comparisonSpeed = Math.min( speed1, linkForComparison.getFreespeed() );

        double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
        double speed = maxBicycleSpeed;
        double gradientSpeed = Math.min( speed, linkWithReducedSpeed.getFreespeed() );

        assertTrue(comparisonSpeed > gradientSpeed);
    }

    @Test
    public void getMaximumVelocityForLink_noSpeedFactor() {

        var link = createLinkWithNoGradientAndNoSpecialSurface();
        var vehicle = createVehicle(link.getFreespeed() + 1, 1);
        var calculator = new SpecificLinkSpeedCalculator();

        double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
        double speed1 = maxBicycleSpeed;
        var speed = Math.min( speed1, link.getFreespeed() );

        assertEquals(link.getFreespeed(), speed, 0.001);
    }

    @Test
    public void getMaximumVelocityForLink_withRoughSurface() {

        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
        Link linkWithCobbleStone = createLink(0, "cobblestone", "not-a-cycle-way", 1.0);

        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);
        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator();

        double maxBicycleSpeed1 = vehicle.getType().getMaximumVelocity();
        double speed1 = maxBicycleSpeed1;
        double comparisonSpeed = Math.min( speed1, linkForComparison.getFreespeed() );

        double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
        double speed = maxBicycleSpeed;
        double gradientSpeed = Math.min( speed, linkWithCobbleStone.getFreespeed() );

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
//        link.getAttributes().putAttribute(BicycleUtils.SURFACE, surfaceType);
//        link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, speedFactor);
//        link.getAttributes().putAttribute(BicycleUtils.WAY_TYPE, wayType);
        return link;
    }

//    @Test
//    public void getMaximumVelocityForLink_withCycleWay() {
//
//        Link linkForComparison = createLinkWithNoGradientAndNoSpecialSurface();
//        Link linkWithCobbleStone = createLink(0, "some-surface", BicycleUtils.CYCLEWAY, 1.0);
//        Vehicle vehicle = createVehicle(linkForComparison.getFreespeed(), 1);
//
//        SpecificLinkSpeedCalculator calculator = new SpecificLinkSpeedCalculator(config);
//
//        double maxBicycleSpeed1 = vehicle.getType().getMaximumVelocity();
//        double speed1 = maxBicycleSpeed1;
//        double comparisonSpeed = Math.min( speed1, linkForComparison.getFreespeed() );
//
//        double maxBicycleSpeed = vehicle.getType().getMaximumVelocity();
//        double speed = maxBicycleSpeed;
//        double gradientSpeed = Math.min( speed, linkWithCobbleStone.getFreespeed() );
//
//        assertEquals(comparisonSpeed, gradientSpeed, 0.0);
//    }

    private static class SpecificLinkSpeedCalculator implements LinkSpeedCalculator {
        SpecificLinkSpeedCalculator( ){

        }
        @Override
        public double getMaximumVelocity(QVehicle qVehicle, Link link, double time) {
            if (isBike(qVehicle)){
                return Math.min( qVehicle.getVehicle().getType().getMaximumVelocity() *1.5, link.getFreespeed() );
            } else{
                return Double.NaN;
                // (this now works because the link speed calculator returns the default for all combinations of (vehicle, link, time) that
                // are not answered by a specialized link speed calculator.  kai, jun'23)
            }

        }

        private boolean isBike(QVehicle qVehicle) {
            final VehicleType vehicleType = qVehicle.getVehicle().getType();
            return vehicleType.getNetworkMode().equals( TransportMode.bike );
        }

    }

}
