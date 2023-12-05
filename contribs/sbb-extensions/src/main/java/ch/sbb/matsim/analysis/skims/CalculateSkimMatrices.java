/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.analysis.skims;

import ch.sbb.matsim.analysis.skims.NetworkSkimMatrices.NetworkIndicators;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Main class to calculate skim matrices. Provides a main-method to be directly started from the command line, but the main-method also acts as a template for custom code using the skims calculation.
 * <p>
 * All calculated matrices are written to files with fixed names (see constants in this class) in an output directory.
 *
 * @author mrieser / SBB
 */
public class CalculateSkimMatrices {

    public static final String CAR_TRAVELTIMES_FILENAME = "car_travel_times.csv.gz";
    public static final String CAR_DISTANCES_FILENAME = "car_distances.csv.gz";
    public static final String PT_DISTANCES_FILENAME = "pt_distances.csv.gz";
    public static final String PT_TRAVELTIMES_FILENAME = "pt_travel_times.csv.gz";
    public static final String PT_ACCESSTIMES_FILENAME = "pt_access_times.csv.gz";
    public static final String PT_EGRESSTIMES_FILENAME = "pt_egress_times.csv.gz";
    public static final String PT_FREQUENCIES_FILENAME = "pt_frequencies.csv.gz";
    public static final String PT_ADAPTIONTIMES_FILENAME = "pt_adaptiontimes.csv.gz";
    public static final String PT_TRAINSHARE_BYDISTANCE_FILENAME = "pt_train_distance_shares.csv.gz";
    public static final String PT_TRAINSHARE_BYTIME_FILENAME = "pt_train_traveltime_shares.csv.gz";
    public static final String PT_TRANSFERCOUNTS_FILENAME = "pt_transfer_counts.csv.gz";
    public static final String BEELINE_DISTANCE_FILENAME = "beeline_distances.csv.gz";
    public static final String ZONE_LOCATIONS_FILENAME = "zone_coordinates.csv";
    private static final Logger log = LogManager.getLogger(CalculateSkimMatrices.class);
    private final static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private final String outputDirectory;
    private final int numberOfThreads;
    private Map<String, Coord[]> coordsPerZone = null;

    public CalculateSkimMatrices(String outputDirectory, int numberOfThreads) {
        this.outputDirectory = outputDirectory;
        File outputDir = new File(outputDirectory);
        if (!outputDir.exists()) {
            log.info("create output directory " + outputDirectory);
            outputDir.mkdirs();
        } else {
            log.warn("output directory exists already, might overwrite data. " + outputDirectory);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException("User does not want to overwrite data.");
            }
        }

        this.numberOfThreads = numberOfThreads;
    }

    private static <T> void combineMatrices(FloatMatrix<T> matrix1, FloatMatrix<T> matrix2) {
        Set<T> ids = matrix2.id2index.keySet();
        for (T fromId : ids) {
            for (T toId : ids) {
                float value2 = matrix2.get(fromId, toId);
                matrix1.add(fromId, toId, value2);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String zonesShapeFilename = args[0];
        String zonesIdAttributeName = args[1];
        String facilitiesFilename = args[2];
        String networkFilename = args[3];
        String transitScheduleFilename = args[4];
        String eventsFilename = args[5];
        String outputDirectory = args[6];
        int numberOfPointsPerZone = Integer.parseInt(args[7]);
        int numberOfThreads = Integer.parseInt(args[8]);
        String[] timesCarStr = args[9].split(";");
        String[] timesPtStr = args[10].split(";");
        Set<String> modes = CollectionUtils.stringToSet(args[11]);

        double[] timesCar = new double[timesCarStr.length];
        for (int i = 0; i < timesCarStr.length; i++) {
            timesCar[i] = Time.parseTime(timesCarStr[i]);
        }

        double[] timesPt = new double[timesPtStr.length];
        for (int i = 0; i < timesPtStr.length; i++) {
            timesPt[i] = Time.parseTime(timesPtStr[i]);
        }

        Config config = ConfigUtils.createConfig();
        Random r = new Random(4711);

        CalculateSkimMatrices skims = new CalculateSkimMatrices(outputDirectory, numberOfThreads);

        skims.calculateSamplingPointsPerZoneFromFacilities(facilitiesFilename, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r, f -> 1);
        skims.writeSamplingPointsToFile(new File(outputDirectory, ZONE_LOCATIONS_FILENAME));

        // alternative if you don't have facilities, use the network:
        // skims.calculateSamplingPointsPerZoneFromNetwork(networkFilename, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r);

        // or load pre-calculated sampling points from an existing file:
        // skims.loadSamplingPointsFromFile("coordinates.csv");

        if (modes.contains(TransportMode.car)) {
            skims.calculateAndWriteNetworkMatrices(networkFilename, eventsFilename, timesCar, config, "", l -> true);
        }

        if (modes.contains(TransportMode.pt)) {
            skims.calculateAndWritePTMatrices(networkFilename,
                    transitScheduleFilename,
                    timesPt[0],
                    timesPt[1],
                    config,
                    "",
                    (line, route) -> route.getTransportMode().equals("train"));
        }

        skims.calculateAndWriteBeelineMatrix();
    }

    public Map<String, Coord[]> getCoordsPerZone() {
        return coordsPerZone;
    }

    public final void calculateSamplingPointsPerZoneFromFacilities(String facilitiesFilename, int numberOfPointsPerZone, String zonesShapeFilename, String zonesIdAttributeName, Random r,
            ToDoubleFunction<ActivityFacility> weightFunction) throws IOException {
        // load facilities
        log.info("loading facilities from " + facilitiesFilename);

        Counter facCounter = new Counter("#");
        List<WeightedCoord> facilities = new ArrayList<>();
        new MatsimFacilitiesReader(null, null, new StreamingFacilities(
                f -> {
                    facCounter.incCounter();
                    double weight = weightFunction.applyAsDouble(f);
                    WeightedCoord wf = new WeightedCoord(f.getCoord(), weight);
                    facilities.add(wf);
                }
        )).readFile(facilitiesFilename);
        facCounter.printCounter();

        selectSamplingPoints(facilities, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r);
    }

    public final void calculateSamplingPointsPerZoneFromNetwork(String networkFilename, int numberOfPointsPerZone, String zonesShapeFilename, String zonesIdAttributeName, Random r)
            throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);
        List<WeightedCoord> weightedNodes = new ArrayList<>(scenario.getNetwork().getNodes().size());
        for (Node node : scenario.getNetwork().getNodes().values()) {
            weightedNodes.add(new WeightedCoord(node.getCoord(), 1));
        }

        selectSamplingPoints(weightedNodes, numberOfPointsPerZone, zonesShapeFilename, zonesIdAttributeName, r);
    }

    public final void selectSamplingPoints(List<WeightedCoord> locations, int numberOfPointsPerZone, String zonesShapeFilename, String zonesIdAttributeName, Random r) throws IOException {
        log.info("loading zones from " + zonesShapeFilename);
        Collection<SimpleFeature> zones = new ShapeFileReader().readFileAndInitialize(zonesShapeFilename);
        SpatialIndex zonesQt = new Quadtree();
        for (SimpleFeature zone : zones) {
            Envelope envelope = ((Geometry) (zone.getDefaultGeometry())).getEnvelopeInternal();
            zonesQt.insert(envelope, zone);
        }

        log.info("assign locations to zones...");
        Map<String, List<WeightedCoord>> allCoordsPerZone = new HashMap<>();
        Counter counter = new Counter("# ");
        for (WeightedCoord loc : locations) {
            counter.incCounter();
            String zoneId = findZone(loc.coord, zonesQt, zonesIdAttributeName);
            if (zoneId != null) {
                allCoordsPerZone.computeIfAbsent(zoneId, k -> new ArrayList<>()).add(loc);
            }
        }
        counter.printCounter();

        // define points per zone
        log.info("choose locations (sampling points) per zone...");

        this.coordsPerZone = new HashMap<>();

        for (Map.Entry<String, List<WeightedCoord>> e : allCoordsPerZone.entrySet()) {
            String zoneId = e.getKey();
            List<WeightedCoord> zoneFacilities = e.getValue();
            double sumWeight = 0.0;
            for (WeightedCoord loc : zoneFacilities) {
                sumWeight += loc.weight;
            }
            Coord[] coords = new Coord[numberOfPointsPerZone];
            for (int i = 0; i < numberOfPointsPerZone; i++) {
                double weight = r.nextDouble() * sumWeight;
                double sum = 0.0;
                WeightedCoord chosenLoc = null;
                for (WeightedCoord loc : zoneFacilities) {
                    sum += loc.weight;
                    if (weight <= sum) {
                        chosenLoc = loc;
                        break;
                    }
                }
                coords[i] = chosenLoc.coord;
            }
            this.coordsPerZone.put(zoneId, coords);
        }
    }

    public void writeSamplingPointsToFile(File file) throws IOException {
        log.info("write chosen coordinates to file " + file.getAbsolutePath());
        try (BufferedWriter writer = IOUtils.getBufferedWriter(file.getAbsolutePath())) {
            writer.write("ZONE;POINT_INDEX;X;Y\n");
            for (Map.Entry<String, Coord[]> e : this.coordsPerZone.entrySet()) {
                String zoneId = e.getKey();
                Coord[] coords = e.getValue();
                for (int i = 0; i < coords.length; i++) {
                    Coord coord = coords[i];
                    writer.write(zoneId);
                    writer.write(";");
                    writer.write(Integer.toString(i));
                    writer.write(";");
                    writer.write(Double.toString(coord.getX()));
                    writer.write(";");
                    writer.write(Double.toString(coord.getY()));
                    writer.write("\n");
                }
            }
        }
    }

    public final void loadSamplingPointsFromFile(String filename) throws IOException {
        log.info("loading sampling points from " + filename);
        String expectedHeader = "ZONE;POINT_INDEX;X;Y";
        this.coordsPerZone = new HashMap<>();
        try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {
            String header = reader.readLine();
            if (!expectedHeader.equals(header)) {
                throw new RuntimeException("Bad header, expected '" + expectedHeader + "', got: '" + header + "'.");
            }
            String line;
            int maxIdx = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = StringUtils.explode(line, ';');
                String zoneId = parts[0];
                int idx = Integer.parseInt(parts[1]);
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                final int length = idx > maxIdx ? idx : maxIdx;
                Coord[] coords = this.coordsPerZone.computeIfAbsent(zoneId, k -> new Coord[length + 1]);
                if (coords.length < (idx + 1)) {
                    Coord[] tmp = new Coord[idx + 1];
                    System.arraycopy(coords, 0, tmp, 0, coords.length);
                    coords = tmp;
                    this.coordsPerZone.put(zoneId, coords);
                }
                coords[idx] = new Coord(x, y);
                if (idx > maxIdx) {
                    maxIdx = idx;
                }
            }
        }
    }

    public final void calculateAndWriteBeelineMatrix() throws IOException {
        log.info("calc beeline distance matrix");
        FloatMatrix<String> beelineMatrix = BeelineDistanceMatrix.calculateBeelineDistanceMatrix(this.coordsPerZone.keySet(), coordsPerZone, numberOfThreads);

        log.info("write beeline distance matrix to " + outputDirectory);
        FloatMatrixIO.writeAsCSV(beelineMatrix, outputDirectory + "/" + BEELINE_DISTANCE_FILENAME);
    }

    public final FloatMatrix<String> calculateBeelineMatrix() {
        log.info("calc beeline distance matrix");
        return BeelineDistanceMatrix.calculateBeelineDistanceMatrix(this.coordsPerZone.keySet(), coordsPerZone, numberOfThreads);
    }

    public final void calculateAndWriteNetworkMatrices(String networkFilename, String eventsFilename, double[] times, Config config, String outputPrefix, Predicate<Link> xy2linksPredicate)
            throws IOException {
        String prefix = outputPrefix == null ? "" : outputPrefix;
        var netIndicators = prepareAndCalculateNetworkMatrices(networkFilename, eventsFilename, times, config, xy2linksPredicate);
        writeNetworkMatricesAsCSV(netIndicators, prefix);
    }

    public final void writeNetworkMatricesAsCSV(NetworkIndicators<String> netIndicators, String prefix) throws IOException {
        log.info("write CAR matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
        FloatMatrixIO.writeAsCSV(netIndicators.travelTimeMatrix, outputDirectory + "/" + prefix + CAR_TRAVELTIMES_FILENAME);
        FloatMatrixIO.writeAsCSV(netIndicators.distanceMatrix, outputDirectory + "/" + prefix + CAR_DISTANCES_FILENAME);
    }

    public final NetworkIndicators<String> prepareAndCalculateNetworkMatrices(String networkFilename, String eventsFilename, double[] times, Config config, Predicate<Link> xy2linksPredicate)  {
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading network from " + networkFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        TravelTime tt;
        if (eventsFilename != null) {
            log.info("extracting actual travel times from " + eventsFilename);
            TravelTimeCalculator ttc = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());
            EventsManager events = new EventsManagerImpl();
            events.addHandler(ttc);
            events.initProcessing();
            new MatsimEventsReader(events).readFile(eventsFilename);
            tt = ttc.getLinkTravelTimes();
        } else {
            tt = new FreeSpeedTravelTime();
            log.info("No events specified. Travel Times will be calculated with free speed travel times.");
        }

        TravelDisutility td = new OnlyTimeDependentTravelDisutility(tt);

        log.info("extracting car-only network");
        final Network carNetwork = NetworkUtils.createNetwork(config);
        new TransportModeNetworkFilter(scenario.getNetwork()).filter(carNetwork, Collections.singleton(TransportMode.car));

        log.info("filter car-only network for assigning links to locations");
        final Network xy2linksNetwork = extractXy2LinksNetwork(carNetwork, xy2linksPredicate, config);

        return calculateNetworkMatrices(times, carNetwork, xy2linksNetwork, tt, td);
    }

    public final NetworkIndicators<String> calculateNetworkMatrices(double[] times, Network carNetwork, Network xy2linksNetwork, TravelTime tt, TravelDisutility td) {
        log.info("calc CAR matrix for " + Time.writeTime(times[0]));
        NetworkIndicators<String> netIndicators = NetworkSkimMatrices.calculateSkimMatrices(
            xy2linksNetwork, carNetwork, coordsPerZone, times[0], tt, td, this.numberOfThreads);

        if (tt instanceof FreeSpeedTravelTime) {
            log.info("Do not calculate CAR matrices for other times as only freespeed is being used");
        } else {
            for (int i = 1; i < times.length; i++) {
                log.info("calc CAR matrices for " + Time.writeTime(times[i]));
                NetworkIndicators<String> indicators2 = NetworkSkimMatrices.calculateSkimMatrices(
                        xy2linksNetwork, carNetwork, coordsPerZone, times[i], tt, td, this.numberOfThreads);
                log.info("merge CAR matrices for " + Time.writeTime(times[i]));
                combineMatrices(netIndicators.travelTimeMatrix, indicators2.travelTimeMatrix);
                combineMatrices(netIndicators.distanceMatrix, indicators2.distanceMatrix);
            }
            log.info("re-scale CAR matrices after all data is merged.");
            netIndicators.travelTimeMatrix.multiply((float) (1.0 / times.length));
            netIndicators.distanceMatrix.multiply((float) (1.0 / times.length));
        }
        return netIndicators;
    }

    private Network extractXy2LinksNetwork(Network network, Predicate<Link> xy2linksPredicate, Config config) {
        Network xy2lNetwork = NetworkUtils.createNetwork(config);
        NetworkFactory nf = xy2lNetwork.getFactory();
        for (Link link : network.getLinks().values()) {
            if (xy2linksPredicate.test(link)) {
                // okay, we need that link
                Node fromNode = link.getFromNode();
                Node xy2lFromNode = xy2lNetwork.getNodes().get(fromNode.getId());
                if (xy2lFromNode == null) {
                    xy2lFromNode = nf.createNode(fromNode.getId(), fromNode.getCoord());
                    xy2lNetwork.addNode(xy2lFromNode);
                }
                Node toNode = link.getToNode();
                Node xy2lToNode = xy2lNetwork.getNodes().get(toNode.getId());
                if (xy2lToNode == null) {
                    xy2lToNode = nf.createNode(toNode.getId(), toNode.getCoord());
                    xy2lNetwork.addNode(xy2lToNode);
                }
                Link xy2lLink = nf.createLink(link.getId(), xy2lFromNode, xy2lToNode);
                xy2lLink.setAllowedModes(link.getAllowedModes());
                xy2lLink.setCapacity(link.getCapacity());
                xy2lLink.setFreespeed(link.getFreespeed());
                xy2lLink.setLength(link.getLength());
                xy2lLink.setNumberOfLanes(link.getNumberOfLanes());
                xy2lNetwork.addLink(xy2lLink);
            }
        }
        return xy2lNetwork;
    }

    public final void calculateAndWritePTMatrices(String networkFilename,
                                                  String transitScheduleFilename,
                                                  double startTime,
                                                  double endTime,
                                                  Config config,
                                                  String outputPrefix,
            BiPredicate<TransitLine, TransitRoute> trainDetector) throws IOException {

        var matrices = calculatePTMatrices(networkFilename,
                transitScheduleFilename, startTime, endTime, config, trainDetector);
        writePTMatricesAsCSV(matrices, outputPrefix);
    }

    public final void writePTMatricesAsCSV(PTSkimMatrices.PtIndicators<String> matrices, String outputPrefix) throws IOException {
        String prefix = outputPrefix == null ? "" : outputPrefix;

        log.info("write PT matrices to " + outputDirectory + (prefix.isEmpty() ? "" : (" with prefix " + prefix)));
        FloatMatrixIO.writeAsCSV(matrices.adaptionTimeMatrix, outputDirectory + "/" + prefix + PT_ADAPTIONTIMES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.frequencyMatrix, outputDirectory + "/" + prefix + PT_FREQUENCIES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.distanceMatrix, outputDirectory + "/" + prefix + PT_DISTANCES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.travelTimeMatrix, outputDirectory + "/" + prefix + PT_TRAVELTIMES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.accessTimeMatrix, outputDirectory + "/" + prefix + PT_ACCESSTIMES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.egressTimeMatrix, outputDirectory + "/" + prefix + PT_EGRESSTIMES_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.transferCountMatrix, outputDirectory + "/" + prefix + PT_TRANSFERCOUNTS_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.trainTravelTimeShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYTIME_FILENAME);
        FloatMatrixIO.writeAsCSV(matrices.trainDistanceShareMatrix, outputDirectory + "/" + prefix + PT_TRAINSHARE_BYDISTANCE_FILENAME);
    }

    public final PTSkimMatrices.PtIndicators<String> calculatePTMatrices(String networkFilename,
                                                                         String transitScheduleFilename,
                                                                         double startTime,
                                                                         double endTime,
                                                                         Config config,
            BiPredicate<TransitLine, TransitRoute> trainDetector) {
        Scenario scenario = ScenarioUtils.createScenario(config);
        log.info("loading schedule from " + transitScheduleFilename);
        new TransitScheduleReader(scenario).readFile(transitScheduleFilename);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilename);

        log.info("prepare PT Matrix calculation");
        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        SwissRailRaptorData raptorData = SwissRailRaptorData.create(scenario.getTransitSchedule(), scenario.getTransitVehicles(), raptorConfig, scenario.getNetwork(), null);
        RaptorParameters raptorParameters = RaptorUtils.createParameters(config);

        log.info("calc PT matrices for " + Time.writeTime(startTime) + " - " + Time.writeTime(endTime));
        PTSkimMatrices.PtIndicators<String> matrices = PTSkimMatrices.calculateSkimMatrices(
                raptorData, this.coordsPerZone, startTime, endTime, 120, raptorParameters, this.numberOfThreads, trainDetector);
        return matrices;

    }

    private String findZone(Coord coord, SpatialIndex zonesQt, String zonesIdAttributeName) {
        Point pt = GEOMETRY_FACTORY.createPoint(new Coordinate(coord.getX(), coord.getY()));
        List elements = zonesQt.query(pt.getEnvelopeInternal());
        for (Object o : elements) {
            SimpleFeature z = (SimpleFeature) o;
            if (((Geometry) z.getDefaultGeometry()).intersects(pt)) {
                return z.getAttribute(zonesIdAttributeName).toString();
            }
        }
        return null;
    }

    private static class WeightedCoord {

        Coord coord;
        double weight;

        private WeightedCoord(Coord coord, double weight) {
            this.coord = coord;
            this.weight = weight;
        }
    }

}
