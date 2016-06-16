//package playground.dziemke.accessibility.ptmatrix;
//
//
//import com.conveyal.gtfs.GTFSFeed;
//import com.conveyal.gtfs.model.Route;
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.gtfs.GtfsConverter;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.controler.OutputDirectoryHierarchy;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//import org.matsim.pt.transitSchedule.api.TransitStopFacility;
//import org.matsim.pt.utils.CreatePseudoNetwork;
//
//import java.io.File;
//import java.time.LocalDate;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Class is deprecated in favor of the GTFS2MATsim "RunGTFS2MATSim" run-script in combination with
// * the ThreadedMatrixCreator from dziemke's playground. These two in combination grant the same functionality.
// * @author gthunig on 25.02.16.
// */
//@Deprecated
//public class CreatePtMatrix {
//
//    private static final Logger log = Logger.getLogger(CreatePtMatrix.class);
//
//    public static void main(String[] args) {
//        final long timeStart = System.currentTimeMillis();
//
////        String gtfsPath = "playgrounds/dziemke/input/sample-feed/sample-feed.zip";
////        String coordinateConversionSystem = "EPSG:26911";
//        String gtfsPath = "playgrounds/dziemke/input/380248.zip";//353413.zip";
//        String coordinateConversionSystem = "EPSG:25832";
////        String networkFile = "examples/pt-tutorial/multimodalnetwork.xml";
//        String outputRoot = "playgrounds/dziemke/output/";
//        File outputFile = new File(outputRoot);
//        if (outputFile.mkdir()) {
//            log.info("Did not found output root at " + outputRoot + " Created it as a new directory.");
//        }
//
//        double departureTime = 8 * 60 * 60;
//
//        Config config = ConfigUtils.createConfig();
//        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
//        Scenario scenario = ScenarioUtils.loadScenario(config);
//        scenario.getConfig().transit().setUseTransit(true);
//
//        CoordinateTransformation ct =
//                TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, coordinateConversionSystem);
//        final GTFSFeed feed = GTFSFeed.fromFile(gtfsPath);
//        for (Route route : feed.routes.values()) {
//            switch (route.route_type) {
//                case 700://bus
//                    route.route_type = 3;
//                    break;
//                case 900://tram
//                    route.route_type = 0;
//                    break;
//                case 109://s-bahn
//                    route.route_type = 1;
//                    break;
//                case 100://rail
//                    route.route_type = 2;
//                    break;
//                case 400://u-bahn
//                    route.route_type = 1;
//                    break;
//                case 1000://ferry
//                    route.route_type = 4;
//                    break;
//                case 102://strange railway
//                    route.route_type = 2;
//                    break;
//                default:
//                    log.warn("Unknown 'wrong' route_type. Value: " + route.route_type + "\nPlease add exception.");
//                    break;
//            }
//        }
//
//        GtfsConverter converter = new GtfsConverter(feed, scenario, ct );
//        converter.setDate(LocalDate.of(2016,5,16));
//        converter.convert();
//
//        CreatePseudoNetwork createPseudoNetwork = new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "");
//        createPseudoNetwork.createNetwork();
//
//        Map<Id<Coord>, Coord> ptMatrixLocationsMap = new HashMap<>();
//
//        int counter = 0;
//        for (TransitStopFacility transitStopFacility: scenario.getTransitSchedule().getFacilities().values()) {
//            if (transitStopFacility.getName().contains("(Berlin)")) {
//                Id<Coord> id = Id.create(transitStopFacility.getId(), Coord.class);
//                Coord coord = transitStopFacility.getCoord();
//                boolean contained = false;
//                for (Coord currentCoord : ptMatrixLocationsMap.values()) {
//                    if (currentCoord.equals(coord)) {
//                        contained = true;
//                    }
//                }
//                if (!contained) {
//                    ptMatrixLocationsMap.put(id, coord);
//                }
//                if (counter++ == 100) {
//                    break;
//                }
//            }
//        }
//        
//        throw new RuntimeException("since the class is deprecated, I refrain from fixing the below (change from coords to facs). kai, may'16") ;
//        /*
//
////        MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
////        networkReader.readFile(networkFile);
//
//        MatrixBasedPtInputUtils.createStopsFile(ptMatrixLocationsMap, outputRoot + "ptStops.csv", ",");
//
//        // The locationFacilitiesMap is passed twice: Once for origins and once for destinations.
//        // In other uses the two maps may be different -- thus the duplication here.
//        log.info("Start matrix-computation...");
//        ThreadedMatrixCreator tmc = new ThreadedMatrixCreator(scenario, ptMatrixLocationsMap,
//                ptMatrixLocationsMap, departureTime, outputRoot, " ", 1);
//
//        //waiting for the output to be written
//        try {
//            tmc.getThread().join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        final long timeEnd = System.currentTimeMillis();
//        System.out.println("Elapsed Time: " + (timeEnd - timeStart) + " millisec.");
//        */
//    }
//
//}