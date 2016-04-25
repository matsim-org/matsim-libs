package playground.dziemke.accessibility.ptmatrix;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.*;
import playground.dziemke.accessibility.ptmatrix.TransitLeastCostPathRouting.TransitRouterImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author gabriel
 * on 19.04.16.
 */
public class NMBValueCheck {

    public static void main(String[] args) {

//        String networkFile = "playgrounds/dziemke/input/NMBM_PT_V1.xml";
//        String transitScheduleFile = "playgrounds/dziemke/input/Transitschedule_PT_V1_WithVehicles.xml";
        String networkFile = "playgrounds/dziemke/input/jtlu14b.output_network.xml";
        String transitScheduleFile = "playgrounds/dziemke/input/jtlu14b.300.transitScheduleScored.xml";

        Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup(), new MatrixBasedPtRouterConfigGroup());
        config.network().setInputFile(networkFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        scenario.getConfig().transit().setUseTransit(true);

        // Read in public transport schedule
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        reader.readFile(transitScheduleFile);
        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        // constructor of TransitRouterImpl needs TransitRouterConfig. This is why it is instantiated here.
        TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig());
        TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);

        Double departureTime = 8. * 60 * 60;
//        Coord origin = new Coord(137547.07266149623,-3706738.5909946687);
//        Coord destination = new Coord(140245.15520623303,-3693657.6437037485);
        Coord origin = new Coord(143583.9441831379, -3699678.99131796);
        Coord destination = new Coord(150583.9441831379,-3699678.99131796);

        CoordinateTransformation coordinateTransformation = TransformationFactory.
                getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
        System.out.println("Origin = " + invertCoord(origin, coordinateTransformation));

        List<Leg> legList = transitRouter.calcRoute(origin, destination, departureTime, null);

        double travelTime = 0d;
        for (Leg leg : legList) {
            if(leg == null) {
                throw new RuntimeException("Leg is null.");
            }
            travelTime += leg.getTravelTime();
            String mode = leg.getMode();
            System.out.println("\nLegMode = " + mode);
            Route legRoute = leg.getRoute();
            if (legRoute != null) {
                Id<Link> startLinkId = legRoute.getStartLinkId();
                Id<Link> endLinkId = legRoute.getEndLinkId();
                System.out.println("StartLinkId " + startLinkId);
                if (startLinkId != null) {
                    System.out.println("StartLink WGS84 coord " +
                            invertCoord(scenario.getNetwork().getLinks().get(startLinkId).getFromNode().getCoord(),
                                    coordinateTransformation));
                    System.out.println("StartLink Alberts coord " +
                            scenario.getNetwork().getLinks().get(startLinkId).getFromNode().getCoord());
                }
                System.out.println("EndLinkId " + endLinkId);
                if (endLinkId != null) {
                    System.out.println("EndLink WGS84 coord " +
                            invertCoord(scenario.getNetwork().getLinks().get(endLinkId).getFromNode().getCoord(),
                                    coordinateTransformation));
                    System.out.println("EndLink Alberts coord " +
                            scenario.getNetwork().getLinks().get(endLinkId).getFromNode().getCoord());
                }
                if (leg.getMode().equals("pt")) {
                    TransitStopFacility startStop = null;
                    TransitStopFacility endStop = null;
                    for (TransitStopFacility stop : transitSchedule.getFacilities().values()) {
                        if (stop.getLinkId().equals(startLinkId)) startStop = stop;
                        if (stop.getLinkId().equals(endLinkId)) endStop = stop;
                    }
                    if (startStop != null && endStop != null) {
                        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
                            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                                TransitRouteStop transitRouteStartStop = transitRoute.getStop(startStop);
                                TransitRouteStop transitRouteEndStop = transitRoute.getStop(endStop);
                                if (transitRouteStartStop != null && transitRouteEndStop != null) {
                                    // that means the routes stops at both of our searched stops
                                    for (Departure departure : transitRoute.getDepartures().values()) {
                                        if (departureTime + travelTime ==
                                                departure.getDepartureTime() + transitRouteEndStop.getArrivalOffset()) {

                                            System.out.println("Route departure time at pt-origin = " +
                                                    departure.getDepartureTime());
                                            System.out.println("Route departure time at entry point = " +
                                                    (departureTime + transitRouteStartStop.getDepartureOffset()));
                                            System.out.println("Route arrival time at exit point = " + (departureTime +
                                                    transitRouteEndStop.getArrivalOffset()));

                                            System.out.println("transitLine = " + transitLine.getId());
                                            System.out.println("transitRoute = " + transitRoute.getId());

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Destination = " + invertCoord(destination, coordinateTransformation));
        System.out.println("final travelTime = " + travelTime);
    }

    private static Coord invertCoord(Coord coord, CoordinateTransformation coordinateTransformation) {
        return new Coord(coordinateTransformation.transform(coord).getY(),
                coordinateTransformation.transform(coord).getX());
    }

}
