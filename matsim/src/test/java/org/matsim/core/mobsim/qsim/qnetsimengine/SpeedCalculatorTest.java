package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpeedCalculatorTest{
    // This originally comes from the bicycle contrib.  One now needs access to AbstractQLink to properly test this functionality. In contrast, the
    // specific bicycle material should not be necessary. kai, jun'23

    @RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();
    private final Config config = ConfigUtils.createConfig();
    private final Network unusedNetwork = NetworkUtils.createNetwork();

	@Test
	void limitedByVehicleSpeed() {
        Link link = createLinkWithNoGradientAndNoSpecialSurface();
        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ), TransportMode.car );
        type.setMaximumVelocity(link.getFreespeed() / 2); // less than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));

        assertTrue( Double.isNaN( new SpecificLinkSpeedCalculator().getMaximumVelocity( vehicle, link, 1 ) ) );
        // (specific link speed calculator is not responsible)

        assertEquals( type.getMaximumVelocity(), getSpeedOnLink( link, vehicle ), 0.0 );
    }

	@Test
	void limitedByLinkSpeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();

        VehicleType type = VehicleUtils.createVehicleType(Id.create("no-bike", VehicleType.class ), TransportMode.car );
        type.setMaximumVelocity(link.getFreespeed() * 2); // _more_ than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));

        assertTrue( Double.isNaN( new SpecificLinkSpeedCalculator().getMaximumVelocity( vehicle, link, 1 ) ) );
        // (specific link speed calculator is not responsible)

        assertEquals( link.getFreespeed(), getSpeedOnLink( link, vehicle ), 0.0 );
    }

	@Test
	void bikeWithSpecificLinkSpeedCalculator() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();

        VehicleType type = VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class ) );
        type.setNetworkMode( TransportMode.bike );
        type.setMaximumVelocity(link.getFreespeed() / 2); // _less_ than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));

        assertEquals( type.getMaximumVelocity()*1.5, new SpecificLinkSpeedCalculator().getMaximumVelocity( vehicle, link, 1 ), 0.0 );
        // (specific link speed calculator _is_ responsible)

        assertEquals( type.getMaximumVelocity()*1.5, getSpeedOnLink( link, vehicle ), 0.0 );
        // (specific link speed calculator uses speed that is larger than maximum vehicle speed)
    }

	@Test
	void bikeLimitedByLinkFreespeed() {

        Link link = createLinkWithNoGradientAndNoSpecialSurface();

        VehicleType type = VehicleUtils.createVehicleType(Id.create("bike", VehicleType.class ) );
        type.setNetworkMode( TransportMode.bike );
        type.setMaximumVelocity(link.getFreespeed() * 2); // _more_ than the link's freespeed
        QVehicle vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId(1), type));

        assertEquals( link.getFreespeed(), new SpecificLinkSpeedCalculator().getMaximumVelocity( vehicle, link, 1 ), 0.0 );
        // (specific link speed calculator _is_ responsible, but reduces to link freespeed)

        assertEquals( link.getFreespeed(), getSpeedOnLink( link, vehicle ), 0.0 );
        // (dto)
    }
    private double getSpeedOnLink( Link link, QVehicle vehicle ){
        config.controller().setOutputDirectory( utils.getOutputDirectory() );
        Scenario scenario = ScenarioUtils.createScenario( config );
        AbstractQSimModule module = new AbstractQSimModule(){
            @Override public void configureQSim(){
                bind( DefaultLinkSpeedCalculator.class ).in( Singleton.class );
                this.addLinkSpeedCalculatorBinding().to( SpecificLinkSpeedCalculator.class ).in( Singleton.class );
            }
        };
        EventsManager eventsManager = EventsUtils.createEventsManager();
        QSim qsim = new QSimBuilder(config).useDefaults().addQSimModule( module ).build(scenario, eventsManager);
        var childInjector = qsim.getChildInjector();

        var qNetworkFactory = childInjector.getInstance( QNetworkFactory.class );
        var qLink = (AbstractQLink) qNetworkFactory.createNetsimLink( link, qNetworkFactory.createNetsimNode( link.getToNode() ) );
        var speedOnLink = qLink.getInternalInterface().getMaximumVelocityFromLinkSpeedCalculator( vehicle, 1. );
        return speedOnLink;
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
