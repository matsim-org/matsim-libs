package org.matsim.application.prepare.freight.optimization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "determine-truck-load",
        description = "Determine the average truck load based on count data",
        showDefaultValues = true
)
public class DetermineAverageTruckLoad implements MATSimAppCommand {
    private static final Logger log = LogManager.getLogger(DetermineAverageTruckLoad.class);

    @CommandLine.Option(names = "--directory", description = "Path to the raw data directory", required = true)
    private Path rawDataDirectory;

    @CommandLine.Option(names = "--working-days", defaultValue = "260", description = "Number of working days in a year")
    private int workingDays;

    @CommandLine.Option(names = "--loads", description = "Average loads of truck to try out, separate with ,",
            defaultValue = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30")
    private String loadsToTryOut;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();  // Use EPSG:5677 for input-crs under current setup

    private final Random random = new Random(1234);

    public static void main(String[] args) {
        new DetermineAverageTruckLoad().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        Path freightDataPath = rawDataDirectory.resolve("ketten-2010.csv");
        Path lookupTablePath = rawDataDirectory.resolve("lookup-table.csv");
        Path pathToBoundaryLinks = rawDataDirectory.resolve("boundary-links.csv");
        Path pathToInternationalRegions = rawDataDirectory.resolve("international-regions.csv");
        Path networkPath = rawDataDirectory.resolve("german-primary-road.network.xml.gz");
        Path shapeFilePath = rawDataDirectory.resolve("NUTS3").resolve("NUTS3_2010_DE.shp");
        Path freightTrafficCountPath = rawDataDirectory.resolve("traffic-count").resolve("freight-count-data.tsv");
        checkIfAllRequiredFilesArePresent(freightDataPath, lookupTablePath, pathToBoundaryLinks,
                pathToInternationalRegions, networkPath, shapeFilePath, freightTrafficCountPath);

        Network network = NetworkUtils.readNetwork(networkPath.toString());
        LeastCostPathCalculator router = createRouter(network);
        ShpOptions shp = new ShpOptions(shapeFilePath, crs.getInputCRS(), StandardCharsets.UTF_8);
        ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "NUTS_ID");
        List<SimpleFeature> features = index.getAll();

        Map<String, Id<Link>> verkehrszelleToLinkIdMapping = createMapping(lookupTablePath, features, network);
        addingInternationalMapping(verkehrszelleToLinkIdMapping, pathToInternationalRegions, pathToBoundaryLinks);

        // Read counting data
        Map<Id<Link>, Double> referenceCounts = new HashMap<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(freightTrafficCountPath),
                CSVFormat.TDF.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String linkIdString = record.get(0);
                double count = Double.parseDouble(record.get(1)) / 2;
                referenceCounts.put(Id.createLinkId(linkIdString), count);
            }
        }
        List<Id<Link>> countingStations = new ArrayList<>(referenceCounts.keySet());

        // Calculate route between any two regions (Or read from the file if exists)
        Path preCalculatedRoutes = rawDataDirectory.resolve("pre-calculated-route.tsv");
        Map<String, Map<String, List<Id<Link>>>> routesMap = new HashMap<>();
        if (Files.exists(preCalculatedRoutes)) {
            loadRoutesMapFromData(preCalculatedRoutes, routesMap, verkehrszelleToLinkIdMapping);
        } else {
            computeRoutesMap(routesMap, network, verkehrszelleToLinkIdMapping, router, countingStations, preCalculatedRoutes);
        }

        // Read data (ketten data)
        List<GoodsFlow> goodsFlows = new ArrayList<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(freightDataPath, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                // Vorlauf
                String modeVL = record.get(6);
                String originVL = record.get(0);
                String destinationVL = record.get(2);
                String tonVL = record.get(15);
                if (checkIfTripIsRelevant(modeVL, originVL, destinationVL, tonVL)) {
                    goodsFlows.add(new GoodsFlow(originVL, destinationVL, Double.parseDouble(tonVL)));
                }

                // Hauptlauf
                String modeHL = record.get(7);
                String originHL = record.get(2);
                String destinationHL = record.get(3);
                String tonHL = record.get(16);
                if (checkIfTripIsRelevant(modeHL, originHL, destinationHL, tonHL)) {
                    goodsFlows.add(new GoodsFlow(originHL, destinationHL, Double.parseDouble(tonHL)));
                }

                // Nachlauf
                String modeNL = record.get(8);
                String originNL = record.get(3);
                String destinationNL = record.get(1);
                String tonNL = record.get(17);
                if (checkIfTripIsRelevant(modeNL, originNL, destinationNL, tonNL)) {
                    goodsFlows.add(new GoodsFlow(originNL, destinationNL, Double.parseDouble(tonNL)));
                }
            }
        }
        int numOfDataEntry = goodsFlows.size();

        String[] dataPoints = loadsToTryOut.split(",");
        Map<Double, Double> sumAbsErrorSummary = new HashMap<>(); // Average truck load --> error
        for (String dataPoint : dataPoints) {
            log.info("Processing: setting average load of truck to " + dataPoint + " ton");
            Map<Id<Link>, Double> actualCounts = new HashMap<>();
            for (Id<Link> linkId : countingStations) {
                actualCounts.put(linkId, 0.0);
            }
            double averageTruckLoad = Double.parseDouble(dataPoint);
            int counter = 0;
            for (GoodsFlow goodFlow : goodsFlows) {
                List<Id<Link>> linksAlongRoute = routesMap.get(goodFlow.getFrom()).get(goodFlow.getTo());
                double numOfTrucks = goodFlow.getTonsPerYear() / (workingDays * averageTruckLoad); // non-integer value is allowed here, as it is a generall analysis
                for (Id<Link> linkId : linksAlongRoute) {
                    double originalValue = actualCounts.get(linkId);
                    actualCounts.put(linkId, originalValue + numOfTrucks);
                }
                counter++;
                if (counter % 100000 == 0) {
                    log.info("Processing: " + counter + " / " + numOfDataEntry);
                }
            }
            double error = calculateTotalError(actualCounts, referenceCounts);
            sumAbsErrorSummary.put(averageTruckLoad, error);
            log.info("This lead to total error of " + error);
        }

        // write down the summary data in csv (also print in the console)
        String resultSummaryPath = rawDataDirectory.toString() + "/average-truck-load-summary.tsv";
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(resultSummaryPath), CSVFormat.TDF);
        tsvWriter.printRecord("average_load", "sum_abs_error");
        for (double load : sumAbsErrorSummary.keySet()) {
            tsvWriter.printRecord(load, sumAbsErrorSummary.get(load));
        }
        tsvWriter.close();
        return 0;
    }

    private void computeRoutesMap(Map<String, Map<String, List<Id<Link>>>> routesMap,
                                  Network network, Map<String, Id<Link>> verkehrszelleToLinkIdMapping,
                                  LeastCostPathCalculator router, List<Id<Link>> countingStations,
                                  Path preCalculatedRoutes) throws IOException {
        log.info("Begin computing the routes map. This will take some time...");
        int counter = 0;
        int total = verkehrszelleToLinkIdMapping.size() * verkehrszelleToLinkIdMapping.size();
        CSVPrinter tsvWriter = new CSVPrinter(new FileWriter(preCalculatedRoutes.toString()), CSVFormat.TDF);
        tsvWriter.printRecord("from", "to", "links (not a single column)");
        List<String> regionsWithoutInformation = new ArrayList<>();
        for (String from : verkehrszelleToLinkIdMapping.keySet()) {
            routesMap.put(from, new HashMap<>());
            for (String to : verkehrszelleToLinkIdMapping.keySet()) {
                Id<Link> fromLink = verkehrszelleToLinkIdMapping.get(from);
                Id<Link> toLink = verkehrszelleToLinkIdMapping.get(to);
                if (fromLink != null && toLink != null) {
                    List<Id<Link>> route = router.calcLeastCostPath(network.getLinks().get(fromLink).getToNode(),
                                    network.getLinks().get(toLink).getToNode(), 0, null, null).links.
                            stream().map(Identifiable::getId).collect(Collectors.toList());
                    route.retainAll(countingStations);
                    routesMap.get(from).put(to, route);
                    List<String> outputRow = new ArrayList<>();
                    outputRow.add(from);
                    outputRow.add(to);
                    List<String> linkIds = route.stream().map(Object::toString).collect(Collectors.toList());
                    outputRow.addAll(linkIds);
                    tsvWriter.printRecord(outputRow);
                } else {
                    if (fromLink == null)
                        regionsWithoutInformation.add(from);
                    if (toLink == null)
                        regionsWithoutInformation.add(to);
                }
                counter++;
                if (counter % 2000 == 0) {
                    log.info("Calculating: " + counter + " / " + total);
                }
            }
        }
        tsvWriter.close();
        for (String region : regionsWithoutInformation) {
            log.warn("We don't have information for region " + region + " in the lookup table");
        }
    }

    private void loadRoutesMapFromData(Path preCalculatedRoutes, Map<String, Map<String, List<Id<Link>>>> routesMap,
                                       Map<String, Id<Link>> verkehrszelleToLinkIdMapping) throws IOException {
        log.info("Loading pre-calculated routes from" + preCalculatedRoutes.toString());
        // Initialize routes map
        for (String from : verkehrszelleToLinkIdMapping.keySet()) {
            routesMap.put(from, new HashMap<>());
            for (String to : verkehrszelleToLinkIdMapping.keySet()) {
                routesMap.get(from).put(to, new ArrayList<>());
            }
        }
        // Read from the pre-calculated data
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(preCalculatedRoutes), CSVFormat.TDF.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String from = record.get(0);
                String to = record.get(1);
                List<Id<Link>> route = new ArrayList<>();
                for (int i = 2; i < record.size(); i++) {
                    route.add(Id.createLinkId(record.get(i)));
                }
                routesMap.get(from).get(to).addAll(route);
            }
        }
    }

    private double calculateTotalError
            (Map<Id<Link>, Double> actualCounts, Map<Id<Link>, Double> referenceCounts) {
        double totalError = 0;
        for (Id<Link> linkId : referenceCounts.keySet()) {
            double error = Math.abs(actualCounts.get(linkId) - referenceCounts.get(linkId));
            totalError += Math.log10(error);
        }
        return totalError;
    }

    private static class GoodsFlow {
        private final String from;
        private final String to;
        private final double tonsPerYear;

        GoodsFlow(String from, String to, double tonsPerYear) {
            this.from = from;
            this.to = to;
            this.tonsPerYear = tonsPerYear;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public double getTonsPerYear() {
            return tonsPerYear;
        }
    }

    private boolean checkIfTripIsRelevant(String mode, String from, String to, String tons) {
        return mode.equals("2") && !from.equals(to) && !tons.equals("0");
    }

    private Map<String, Id<Link>> createMapping(Path lookupTablePath, List<SimpleFeature> features, Network network) throws IOException {
        Map<String, Id<Link>> verkehrszelleToLinkIdMapping = new HashMap<>();
        Map<String, Id<Link>> nutsLinkIdMap = new HashMap<>();
        for (SimpleFeature feature : features) {
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coord centroidCoord = MGC.point2Coord(geometry.getCentroid());
            Link link = NetworkUtils.getNearestLink(network, centroidCoord);
            assert link != null;
            nutsLinkIdMap.put((String) feature.getAttribute("NUTS_ID"), link.getId());
        }
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(lookupTablePath, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String zelle = record.get(0);
                String nuts = record.get(3);
                verkehrszelleToLinkIdMapping.put(zelle, nutsLinkIdMap.get(nuts));
            }
        }
        return verkehrszelleToLinkIdMapping;
    }

    private void addingInternationalMapping(Map<String, Id<Link>> verkehrszelleToLinkIdMapping,
                                            Path pathToInternationalRegions, Path pathToBoundaryLinks) throws IOException {
        // Read border links
        Map<String, Id<Link>> orientationToLinkMapping = new HashMap<>();
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(pathToBoundaryLinks),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                orientationToLinkMapping.put(record.get(1), Id.createLinkId(record.get(0)));
            }
        }
        assert orientationToLinkMapping.size() == 8 :
                "Currently we have in total 8 orientations. Each orientation should contain at least 1 link!";

        // Read international regions and add mappings to the map
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(pathToInternationalRegions, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                String zelle = record.get(0);
                String orientation = record.get(2);
                Id<Link> linkId = orientationToLinkMapping.get(orientation);
                verkehrszelleToLinkIdMapping.put(zelle, linkId);
            }
        }
    }

    private LeastCostPathCalculator createRouter(Network network) {
        Config config = ConfigUtils.createConfig();
        config.plansCalcRoute().setRoutingRandomness(0);
        TravelTime travelTime = new FreeSpeedTravelTime();
        TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory
                (TransportMode.car, config).createTravelDisutility(travelTime);
        return new SpeedyALTFactory().
                createPathCalculator(network, travelDisutility, travelTime);
    }


    private void checkIfAllRequiredFilesArePresent(Path freightDataPath,
                                                   Path lookupTablePath, Path pathToBoundaryLinks,
                                                   Path pathToInternationalRegions, Path networkPath,
                                                   Path shapeFilePath, Path freightCountPath) {
        List<Path> paths = List.of(freightCountPath, lookupTablePath, pathToBoundaryLinks,
                pathToInternationalRegions, networkPath, shapeFilePath, freightCountPath);

        for (Path path : paths) {
            if (!Files.exists(path)) {
                log.error("Required data {} not found", path);
                throw new RuntimeException("Required files are missing. Aborting...");
            }
        }
    }
}
