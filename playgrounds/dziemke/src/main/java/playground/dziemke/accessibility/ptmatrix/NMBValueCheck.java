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
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.pt.router.TransitRouterImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        String outputFilePathLegEnds = "playgrounds/dziemke/output/routedPathLegEnds.csv";
        String outputFilePathPtStops = "playgrounds/dziemke/output/routedPathPtStops.csv";

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
        transitRouterConfig.setSearchRadius(20000);
//        System.out.println("additional tranfer time = " + transitRouterConfig.getAdditionalTransferTime());
        TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);

        Double departureTime = 8. * 60 * 60;
//        Coord origin = new Coord(137547.07266149623,-3706738.5909946687);
//        Coord destination = new Coord(140245.15520623303,-3693657.6437037485);
//        Coord origin = new Coord(143583.9441831379, -3699678.99131796);jtlu14b
//        Coord destination = new Coord(150583.9441831379,-3699678.99131796);
        Coord origin = new Coord(111583.94418313791,-3714678.99131796);
        Coord destination = new Coord(153583.9441831379,-3688678.99131796);

        CoordinateTransformation coordinateTransformation = TransformationFactory.
                getCoordinateTransformation(TransformationFactory.WGS84_SA_Albers, TransformationFactory.WGS84);
        System.out.print("\n\nOrigin = " + invertCoord(origin, coordinateTransformation));
        System.out.println("StartLink Alberts origin " + origin);

        List<Leg> legList = transitRouter.calcRoute(new FakeFacility(origin), new FakeFacility(destination), departureTime, null);
        ArrayList<Coord> legEnds = new ArrayList<>();
        ArrayList<Coord> ptStops = new ArrayList<>();
        legEnds.add(origin);

        double travelTime = 0d;
        for (Leg leg : legList) {
            if(leg == null) {
                throw new RuntimeException("Leg is null.");
            }
            travelTime += leg.getTravelTime();
            String mode = leg.getMode();
            System.out.println("\nLegMode = " + mode);
            System.out.println("Leg travel time = " + leg.getTravelTime());
            System.out.println("Aggregated travel time = " + travelTime);
            Route legRoute = leg.getRoute();
            if (legRoute != null) {
                Id<Link> startLinkId = legRoute.getStartLinkId();
                Id<Link> endLinkId = legRoute.getEndLinkId();
                System.out.println("StartLinkId " + startLinkId);
                if (startLinkId != null) {
                    Coord coord = scenario.getNetwork().getLinks().get(startLinkId).getCoord();
                    System.out.println("StartLink WGS84 coord " + invertCoord(coord, coordinateTransformation));
                    System.out.println("StartLink Alberts coord " + coord);
                }
                System.out.println("EndLinkId " + endLinkId);
                if (endLinkId != null) {
                    Coord coord = scenario.getNetwork().getLinks().get(endLinkId).getToNode().getCoord();
                    legEnds.add(coord);
                    System.out.println("EndLink WGS84 coord " + invertCoord(coord, coordinateTransformation));
                    System.out.println("EndLink Alberts coord " + coord);
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
                                TransitRouteStop boardingStop = transitRoute.getStop(startStop);
                                TransitRouteStop alightingStop = transitRoute.getStop(endStop);
                                if (boardingStop != null && alightingStop != null) {
                                    // that means the routes stops at both of our searched stops
                                    for (Departure departure : transitRoute.getDepartures().values()) {
                                        if (departureTime + travelTime ==
                                                departure.getDepartureTime() + alightingStop.getArrivalOffset()) {
                                            if (departure.getDepartureTime() + boardingStop.getDepartureOffset()
                                                    < (travelTime - leg.getTravelTime()) ||
                                                    departure.getDepartureTime() + alightingStop.getArrivalOffset()
                                                            > travelTime) {
                                                for (TransitRouteStop stop : transitRoute.getStops()) {
                                                    if (stop.getStopFacility() == startStop &&
                                                            departure.getDepartureTime() + stop.getDepartureOffset()
                                                                >= (travelTime - leg.getTravelTime())) {
                                                        boardingStop = stop;
                                                    }
                                                    if (stop.getStopFacility() == endStop &&
                                                            departure.getDepartureTime() + stop.getArrivalOffset()
                                                                    <= travelTime) {
                                                        alightingStop = stop;
                                                    }
                                                }
                                            }
                                            NetworkRoute route = transitRoute.getRoute().getSubRoute(startLinkId, endLinkId);
                                            ptStops.addAll(route.getLinkIds().stream().map(linkId ->
                                                    scenario.getNetwork().getLinks().get(linkId).getCoord()).
                                                    collect(Collectors.toList()));
                                            System.out.println("Route departure time at pt-origin = " +
                                                    departure.getDepartureTime());
                                            System.out.println("Route departure time at boarding stop = " +
                                                    (departure.getDepartureTime() + boardingStop.getDepartureOffset()));
                                            System.out.println("Route arrival time at alighting stop = " +
                                                    (departure.getDepartureTime() + alightingStop.getArrivalOffset()));
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
        legEnds.add(destination);
        System.out.println("Destination = " + invertCoord(destination, coordinateTransformation));
        System.out.println("StartLink Alberts origin " + destination);
        System.out.println("\nfinal travelTime = " + travelTime + "\n\n");

        writeOutCoords(outputFilePathLegEnds, legEnds);
        writeOutCoords(outputFilePathPtStops, ptStops);
    }

    private static void writeOutCoords(String outputFilePath, ArrayList<Coord> coords) {
        CSVFileWriter writer = new CSVFileWriter(outputFilePath, ";");
        writer.writeField("x");
        writer.writeField("y");
        for (Coord coord : coords) {
            writer.writeNewLine();
            writer.writeField(coord.getX());
            writer.writeField(coord.getY());
        }
        writer.close();
    }

    private static Coord invertCoord(Coord coord, CoordinateTransformation coordinateTransformation) {
        return new Coord(coordinateTransformation.transform(coord).getY(),
                coordinateTransformation.transform(coord).getX());
    }

}
