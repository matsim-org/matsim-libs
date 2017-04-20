//package playground.dziemke.accessibility.OTPMatrix;
//
//import com.vividsolutions.jts.geom.Coordinate;
//
//import org.matsim.api.core.v01.Coord;
//import org.matsim.core.utils.geometry.CoordUtils;
//import org.matsim.core.utils.geometry.CoordinateTransformation;
//import org.matsim.core.utils.geometry.transformations.TransformationFactory;
//import org.opentripplanner.analyst.batch.Individual;
//import org.opentripplanner.analyst.core.Sample;
//import org.opentripplanner.analyst.request.SampleFactory;
//import org.opentripplanner.common.model.GenericLocation;
//import org.opentripplanner.routing.algorithm.AStar;
//import org.opentripplanner.routing.core.OptimizeType;
//import org.opentripplanner.routing.core.RoutingRequest;
//import org.opentripplanner.routing.core.State;
//import org.opentripplanner.routing.core.TraverseModeSet;
//import org.opentripplanner.routing.graph.Edge;
//import org.opentripplanner.routing.graph.Graph;
//import org.opentripplanner.routing.spt.ShortestPathTree;
//import org.opentripplanner.standalone.OTPMain;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
///**
// * A Router that uses org.opentripplanner from Conveyal to route and write out a Matrix of traveltimes and
// *  another Matrix of traveldistances.
// * For access call the Main with the there declared arguments to launch it.
// *
// * @author gthunig
// */
//public class OTPMatrixRouter {
//
//    private static final Logger log = LoggerFactory.getLogger(OTPMatrixRouter.class);
//
//    public static void main(String[] args) {
//
//        if (args.length != 9) {
//            log.error("The args schould contain 9 parameters: " +
//                    "1.fromIndividualsFilePath " +
//                    "2.toIndividualsFilePath " +
//                    "3.graphParentDirecoryPath " +
//                    "4.outputDirectory " +
//                    "5.timeZone " +
//                    "6.date " +
//                    "7.departureTime " +
//                    "8.inputCRS for output Conversion " +
//                    "9.outputCRS for output Conversion ");
//        } else {
//        	CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(args[7], args[8]);
//
//        	List<Individual> fromIndividuals = readIndividuals(args[0]);
//            List<Individual> toIndividuals = readIndividuals(args[1]);
//
//            long startTime = System.currentTimeMillis();
//            routeMatrix(fromIndividuals, toIndividuals, ct, args[2], args[3], args[4], args[5], Integer.parseInt(args[6]));
//            long endTime = System.currentTimeMillis();
//            long elapsedTime = (endTime - startTime) / 1000;
//            int hours = (int) elapsedTime / 3600;
//            int minutes = (int) (elapsedTime - hours * 3600) / 60;
//            int seconds = (int) (elapsedTime - hours * 3600 - minutes * 60);
//            log.info("Elapsed time = " + hours + ":" + minutes + ":" + seconds);
//        }
//    }
//
//	/**
//     * creates a directory if it doesen't exist jet
//     *
//     * @param outputDir the directory to check
//     */
//    private static void mkdir(String outputDir) {
//        if (new File(outputDir).mkdir()) {
//            log.info("Did not found outputRoot at " + outputDir + " Created it as a new directory.");
//        }
//    }
//
//    private static List<Individual> readIndividuals(String fileName) {
//        CSVReader reader = new CSVReader(fileName, ",");
//
//        List<Individual> individuals = new ArrayList<>();
//        String[] line = reader.readLine();
//        int columnCount = line.length;
//        // every stop.txt from a gtfsfeed has to have "stop_id", "stop_lon" and "stop_lat" as columns
//        int idColumn = CSVReader.getColumnNumber(line, "stop_id");
//        int lonColumn = CSVReader.getColumnNumber(line, "stop_lon");
//        int latColumn = CSVReader.getColumnNumber(line, "stop_lat");
//        line = reader.readLine();
//        while (line != null) {
//        	int currentLonColumn;
//        	int currentLatColumn;
//            if (line.length == columnCount ) {
//                currentLonColumn = lonColumn;
//                currentLatColumn = latColumn;
//            } else if (line.length == columnCount + 1) {
//                //assuming that "stop_name" contains a ","
//                currentLonColumn = lonColumn + 1;
//                currentLatColumn = latColumn + 1;
//            } else {
//                break;
//            }
//            Individual individual = new Individual(line[idColumn], Double.parseDouble(line[currentLonColumn]), Double.parseDouble(line[currentLatColumn]), 0);
//            individuals.add(individual);
//            line = reader.readLine() ;
//        }
//        log.info("Found " + individuals.size() + " coordinates.");
//
//        return individuals;
//    }
//
//    private static void routeMatrix(List<Individual> fromindividuals, List<Individual> toIndividuals,
//    		CoordinateTransformation ct, String graphDir, String outputDir, String timeZone, String date, int departureTime) {
//
//        buildGraph(graphDir);
//        Graph graph = loadGraph(graphDir);
//        assert graph != null;
//
//        Calendar calendar = prepareCalendarSettings(timeZone, date, departureTime);
//
//        mkdir(outputDir);
//
//        routeMatrix(graph, calendar, indexIndividuals(graph, fromindividuals, ct, outputDir + "fromIDs.csv"), indexIndividuals(graph, toIndividuals, ct, outputDir + "toIDs.csv"), outputDir);
//        log.info("Shutdown");
//    }
//
//    static boolean buildGraph(String graphDir) {
//        if (!new File(graphDir + "Graph.obj").exists()) {
//            log.info("No graphfile found. Building the graph from content from: " + new File(graphDir).getAbsolutePath() + " ...");
//            OTPMain.main(new String[]{"--build", graphDir});
//            log.info("Building the graph finished.");
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    static Graph loadGraph(String inputRoot) {
//
//        log.info("Loading the graph...");
//        try {
//            Graph graph = Graph.load(new File(inputRoot + "Graph.obj"), Graph.LoadLevel.FULL);
//            log.info("Loading the graph finished.");
//            return graph;
//        } catch (IOException | ClassNotFoundException e) {
//            log.info("Error while loading the Graph.");
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//    private static Calendar prepareCalendarSettings(String timeZone, String date, int departureTime) {
//        log.info("Preparing settings for routing...");
//        final Calendar calendar = Calendar.getInstance();
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
//        TimeZone tZ = TimeZone.getTimeZone(timeZone);
//        df.setTimeZone(tZ);
//        calendar.setTimeZone(tZ);
//        try {
//            calendar.setTime(df.parse(date));
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//        calendar.add(Calendar.SECOND, departureTime);
//        log.info("Preparing settings for routing finished.");
//        return calendar;
//    }
//
//    private static List<Individual> indexIndividuals(Graph graph, List<Individual> individuals, CoordinateTransformation ct, String filePath) {
//        log.info("Start indexing vertices and writing them out...");
//        InputsCSVWriter individualsWriter = new InputsCSVWriter(filePath, ",");
//        SampleFactory sampleFactory = graph.getSampleFactory();
//        int counter = 0;
//        for (int i = 0; i < individuals.size(); i++) {
//            Individual individual = individuals.get(i);
//            Sample sample = sampleFactory.getSample(individual.lon, individual.lat);
//            if (sample == null) {
//                counter++;
//                individuals.remove(individual);
//                i--;
//                continue;
//            }
//            Coord inputCoord = CoordUtils.createCoord(individual.lon, individual.lat);
//            Coord outputCoord = ct.transform(inputCoord);
//
//            individual.sample = sample;
//            individualsWriter.writeField(individual.label);
////			individualsWriter.writeField(individual.lat);
////			individualsWriter.writeField(individual.lon);
//            individualsWriter.writeField(outputCoord.getX());
//            individualsWriter.writeField(outputCoord.getY());
//            individualsWriter.writeNewLine();
//        }
//        log.info("Left out " + counter + " individuals because no sample could be found for their coordinates. Probably out of bounds.");
//        log.info(individuals.size() + " individuals will be used for computation.");
//        individualsWriter.close();
//        log.info("Indexing vertices and writing them out: done.");
//        return individuals;
//    }
//
//    private static RoutingRequest getRoutingRequest(Graph graph, Calendar calendar, Individual individual) {
//        TraverseModeSet modeSet = new TraverseModeSet();
//        modeSet.setWalk(true);
//        modeSet.setTransit(true);
////		    modeSet.setBicycle(true);
//        RoutingRequest request = new RoutingRequest(modeSet);
//        request.setWalkBoardCost(3 * 60); // override low 2-4 minute values
//        request.setBikeBoardCost(3 * 60 * 2);
//        request.setOptimize(OptimizeType.QUICK);
//        request.setMaxWalkDistance(Double.MAX_VALUE);
//        request.batch = true;
//        request.setDateTime(calendar.getTime());
//        request.from = new GenericLocation(individual.lat, individual.lon);
//        try {
//            request.setRoutingContext(graph);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            log.error("Latitude  = " + individual.lat);
//            log.error("Longitude = " + individual.lon);
//            return null;
//        }
//        return request;
//    }
//
//    private static void routeMatrix(Graph graph, Calendar calendar, List<Individual> fromIndividuals, List<Individual> toIndividuals, String outputDir) {
//        log.info("Start routing...");
//        InputsCSVWriter timeWriter = new InputsCSVWriter(outputDir + "tt.csv", " ");
//        InputsCSVWriter distanceWriter = new InputsCSVWriter(outputDir + "td.csv", " ");
//
//        for (int i = 0; i < fromIndividuals.size(); i++) {
//            Individual fromIndividual = fromIndividuals.get(i);
//            long t0 = System.currentTimeMillis();
//
//            RoutingRequest request = getRoutingRequest(graph, calendar, fromIndividual);
//            if (request == null) continue;
//            ShortestPathTree spt = (new AStar()).getShortestPathTree(request);
//            if (spt != null) {
//                for (Individual toIndividual : toIndividuals) {
//                    timeWriter.writeField(fromIndividual.label);
//                    timeWriter.writeField(toIndividual.label);
//                    distanceWriter.writeField(fromIndividual.label);
//                    distanceWriter.writeField(toIndividual.label);
//                    if (fromIndividual.equals(toIndividual)) {
//                        timeWriter.writeField(0);
//                        distanceWriter.writeField(0);
//                    } else {
//                        route(toIndividual, spt, timeWriter, distanceWriter);
//                    }
//                    timeWriter.writeNewLine();
//                    distanceWriter.writeNewLine();
//                }
//
//            }
//            long t1 = System.currentTimeMillis();
//            log.info((i+1) + "/" + fromIndividuals.size() + " Millis: " + (t1-t0));
//        }
//        timeWriter.close();
//        distanceWriter.close();
//        log.info("Routing finished");
//    }
//
//    private static void route(Individual toIndividual, ShortestPathTree spt, InputsCSVWriter timeWriter, InputsCSVWriter distanceWriter) {
//
//        List<State> states = eval(spt, toIndividual.sample);
//
//        long elapsedTime = Long.MAX_VALUE;
//        double distance = 0;
//
//        for (State state : states) {
//            Edge backEdge = state.getBackEdge();
//            if (backEdge != null && backEdge.getFromVertex() != null) {
//                distance += backEdge.getDistance();
//                elapsedTime = state.getActiveTime();
//            }
//        }
//
//        //write output
//        timeWriter.writeField(elapsedTime);
//        distanceWriter.writeField(distance);
//    }
//
//    static long getSingleRouteTime(Graph graph, Calendar calendar, Coordinate origin, Coordinate destination) {
//        if (origin.equals(destination)) return 0;
//        assert graph != null;
//
//        Individual startIndividual = new Individual("origin", origin.y, origin.x, 0);
//        RoutingRequest routingRequest = getRoutingRequest(graph, calendar, startIndividual);
//
//        ShortestPathTree spt = (new AStar()).getShortestPathTree(routingRequest);
//        if (spt != null) {
//            SampleFactory sampleFactory = graph.getSampleFactory();
//            Sample sample = sampleFactory.getSample(destination.y, destination.x);
//            List<State> states = eval(spt, sample);
//            long elapsedTime = Long.MAX_VALUE;
//
//            for (State state : states) {
//                Edge backEdge = state.getBackEdge();
//                if (backEdge != null && backEdge.getFromVertex() != null) {
//                    elapsedTime = state.getActiveTime();
//                }
//            }
//            return elapsedTime;
//        }
//        return -1;
//    }
//
//    private static List<State> eval(ShortestPathTree spt, Sample sample) {
//        State s0 = spt.getState(sample.v0);
//        State s1 = spt.getState(sample.v1);
//        long m0 = Long.MAX_VALUE;
//        long m1 = Long.MAX_VALUE;
//        double walkSpeed = spt.getOptions().walkSpeed;
//        if (s0 != null)
//            m0 = (int)(s0.getActiveTime() + sample.d0 / walkSpeed);
//        if (s1 != null)
//            m1 = (int)(s1.getActiveTime() + sample.d1 / walkSpeed);
//        State bestState = (m0<m1)?s0:s1;
//        LinkedList<State> states = new LinkedList<>();
//
//        for(State cur = bestState; cur != null; cur = cur.getBackState()) {
//            states.addFirst(cur);
//        }
//
//        return states;
//    }
//}
