package org.matsim.application.prepare.freight;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.LanduseOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This code generates German wide long distance freight trip.
 * Input: Freight data; German major road network; NUTS3 shape file; Look up
 * table between NUTS zone ID and Verkehrszellen
 * <p>
 * Author: Chengqi Lu
 */
@CommandLine.Command(
        name = "generate-german-freight-trips",
        description = "Generate german wide freight population",
        showDefaultValues = true
)
public class GenerateGermanWideFreightTrips implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(GenerateGermanWideFreightTrips.class);

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to the freight data directory")
    private Path rawDataDirectory;

    @CommandLine.Option(names = "--network", description = "Path to desired network file", required = true)
    private Path networkPath;

    @CommandLine.Option(names = "--sample", defaultValue = "1", description = "Scaling factor of the freight traffic (0, 1)", required = true)
    private double sample;

    @CommandLine.Option(names = "--truck-load", defaultValue = "7.0", description = "Average load of truck", required = true)
    private double averageTruckLoad;

    @CommandLine.Option(names = "--working-days", defaultValue = "260", description = "Number of working days in a year", required = true)
    private int workingDays;

    @CommandLine.Option(names = "--output", description = "Path to output population", required = true)
    private Path output;

    @CommandLine.Option(names = "--boundary-links", description = "Path to boundary links for cross-broader traffic", required = false)
    private Path pathToBoundaryLinks;

    @CommandLine.Mixin
    private LanduseOptions landuse = new LanduseOptions();

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    private final SplittableRandom rnd = new SplittableRandom(4711);

    public static void main(String[] args) {
        System.exit(new CommandLine(new GenerateGermanWideFreightTrips()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        Path shapeFilePath = rawDataDirectory.resolve("NUTS3").resolve("NUTS3_2010_DE.shp");
        Path freightDataPath = rawDataDirectory.resolve("ketten-2010.csv");
        Path lookupTablePath = rawDataDirectory.resolve("lookup-table.csv");

        if (!Files.exists(shapeFilePath)) {
            log.error("Required shape file {} not found", shapeFilePath);
        }

        if (!Files.exists(freightDataPath)) {
            log.error("Required freight data {} not found", freightDataPath);
        }

        if (!Files.exists(lookupTablePath)) {
            log.error("Required lookup table {} not found", lookupTablePath);
        }

        double adjustedTrucksLoad = averageTruckLoad * (1 / sample) * workingDays; // 1 year = x working days

        // Load config, scenario and network
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem(crs.getInputCRS());
        config.network().setInputFile(networkPath.toString());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        List<Link> links = network.getLinks().values().stream().filter(l -> l.getAllowedModes().contains("car"))
                .collect(Collectors.toList());

        ShpOptions shp = new ShpOptions(shapeFilePath, "EPSG:5677", StandardCharsets.UTF_8);

        // Extracting relevant zones and associate them with the all the links inside
        log.info("Analyzing regions in the Shapefile (this may take some time)...");
        Map<String, List<Id<Link>>> regionLinksMap = new HashMap<>();

        ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "NUTS_ID");
        ShpOptions.Index landIndex = landuse.getIndex(crs.getInputCRS());

        for (Link link : links) {
            String nutsId = index.query(link.getToNode().getCoord());
            if (nutsId != null) {
                // filter additional links by landuse
                if (landIndex != null) {
                    if (!landIndex.contains(link.getToNode().getCoord()))
                        continue;
                }
                regionLinksMap.computeIfAbsent(nutsId, (k) -> new ArrayList<>()).add(link.getId());
            }
        }

        // For regions without any industrial area, then any links inside the region may be chosen
        Set<String> completedRegions = new HashSet<>();
        completedRegions.addAll(regionLinksMap.keySet());
        if (landIndex != null) {
            for (Link link : links) {
                String nutsId = index.query(link.getToNode().getCoord());
                if (nutsId != null && !completedRegions.contains(nutsId)) {
                    regionLinksMap.computeIfAbsent(nutsId, (k) -> new ArrayList<>()).add(link.getId());
                }
            }
        }

        // Reading the look up table (RegionID-RegionName-Table.csv)
        Map<String, String> lookUpTable = new HashMap<>();

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(lookupTablePath),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {
                lookUpTable.put(record.get(0), record.get(3));
            }
        }

        log.info("Region analysis complete!");
        log.info("There are {} relevant regions", regionLinksMap.keySet().size());

        Set<String> relevantRegionNutsIds = regionLinksMap.keySet();
        Map<String, String> lookUpTableCore = new HashMap<>();
        for (String regionId : lookUpTable.keySet()) {
            if (relevantRegionNutsIds.contains(lookUpTable.get(regionId))) {
                lookUpTableCore.put(regionId, lookUpTable.get(regionId));
            }
        }
        Set<String> relevantRegionIds = lookUpTableCore.keySet();

        MutableInt totalGeneratedPerson = new MutableInt();

        // Analyze cross broader trips
        boolean includeInternationalTrips = false;
        List<Id<Link>> boundaryLinkIds = new ArrayList<>();
        if (pathToBoundaryLinks != null) {
            includeInternationalTrips = true;
            try (BufferedReader csvReader = Files.newBufferedReader(pathToBoundaryLinks)){
                String[] linksIdStrings = csvReader.readLine().split(",");
                for (String linkIdString : linksIdStrings) {
                    boundaryLinkIds.add(Id.createLinkId(linkIdString));
                }
            }
        }

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(freightDataPath, StandardCharsets.ISO_8859_1),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {

            for (CSVRecord record : parser) {

                String goodType = record.get(10);

                // Vorlauf
                String modeVL = record.get(6);
                String originVL = record.get(0);
                String destinationVL = record.get(2);
                String tonVL = record.get(15);

                // Hauptlauf
                String modeHL = record.get(7);
                String originHL = record.get(2);
                String destinationHL = record.get(3);
                String tonHL = record.get(16);

                // Nachlauf
                String modeNL = record.get(8);
                String originNL = record.get(3);
                String destinationNL = record.get(1);

                String tonNL = record.get(17);

                if ((relevantRegionIds.contains(originVL) && relevantRegionIds.contains(destinationVL)) || includeInternationalTrips) {
                    if (modeVL.equals("2") && !tonVL.equals("0")) {
                        double trucks = Double.parseDouble(tonVL) / adjustedTrucksLoad;
                        int numOfTrucks = 0;
                        if (trucks < 1) {
                            if (rnd.nextDouble() < trucks) {
                                numOfTrucks = 1;
                            }
                        } else {
                            numOfTrucks = (int) (Math.floor(trucks) + 1);
                        }
                        List<Id<Link>> linksInOrigin = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(originVL), boundaryLinkIds);
                        Id<Link> fromLinkId = linksInOrigin.get(rnd.nextInt(linksInOrigin.size()));

                        List<Id<Link>> linksInDestination = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(destinationVL), boundaryLinkIds);
                        Id<Link> toLinkId = linksInDestination.get(rnd.nextInt(linksInDestination.size()));

                        generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                                populationFactory, totalGeneratedPerson);
                    }
                }

                if ((relevantRegionIds.contains(originHL) && relevantRegionIds.contains(destinationHL)) || includeInternationalTrips) {
                    if (modeHL.equals("2") && !tonHL.equals("0")) {
                        double trucks = Double.parseDouble(tonHL) / adjustedTrucksLoad;
                        int numOfTrucks = 0;
                        if (trucks < 1) {
                            if (rnd.nextDouble() < trucks) {
                                numOfTrucks = 1;
                            }
                        } else {
                            numOfTrucks = (int) (Math.floor(trucks) + 1);
                        }
                        List<Id<Link>> linksInOrigin = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(originHL), boundaryLinkIds);
                        Id<Link> fromLinkId = linksInOrigin.get(rnd.nextInt(linksInOrigin.size()));

                        List<Id<Link>> linksInDestination = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(destinationHL), boundaryLinkIds);
                        Id<Link> toLinkId = linksInDestination.get(rnd.nextInt(linksInDestination.size()));

                        generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                                populationFactory, totalGeneratedPerson);
                    }
                }

                if ((relevantRegionIds.contains(originNL) && relevantRegionIds.contains(destinationNL)) || includeInternationalTrips) {
                    if (modeNL.equals("2") && !tonNL.equals("0")) {
                        double trucks = Double.parseDouble(tonNL) / adjustedTrucksLoad;
                        int numOfTrucks = 0;
                        if (trucks < 1) {
                            if (rnd.nextDouble() < trucks) {
                                numOfTrucks = 1;
                            }
                        } else {
                            numOfTrucks = (int) (Math.floor(trucks) + 1);
                        }
                        List<Id<Link>> linksInOrigin = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(originNL), boundaryLinkIds);
                        Id<Link> fromLinkId = linksInOrigin.get(rnd.nextInt(linksInOrigin.size()));

                        List<Id<Link>> linksInDestination = regionLinksMap.
                                getOrDefault(lookUpTableCore.get(destinationNL), boundaryLinkIds);
                        Id<Link> toLinkId = linksInDestination.get(rnd.nextInt(linksInDestination.size()));

                        generateFreightPlan(network, fromLinkId, toLinkId, numOfTrucks, goodType, population,
                                populationFactory, totalGeneratedPerson);
                    }
                }
            }
        }

        // Write population
        log.info("Writing population file...");
        log.info("There are in total " + population.getPersons().keySet().size() + " freight trips");

        PopulationUtils.writePopulation(population, output.toString());

        return 0;
    }


    private void generateFreightPlan(Network network, Id<Link> fromLinkId, Id<Link> toLinkId, int numOfTrucks,
                                     String goodType, Population population, PopulationFactory populationFactory,
                                     MutableInt totalGeneratedPersons) {
        if (fromLinkId.toString().equals(toLinkId.toString())) {
            return; // We don't have further information on the trips within the same region
        }

        int generated = 0;
        while (generated < numOfTrucks) {
            Person freightPerson = populationFactory.createPerson(
                    Id.create("freight_" + totalGeneratedPersons.intValue(), Person.class));
            freightPerson.getAttributes().putAttribute("subpopulation", "freight");
            freightPerson.getAttributes().putAttribute("type_of_good", goodType);

            Plan plan = populationFactory.createPlan();
            Activity act0 = populationFactory.createActivityFromLinkId("freight_start", fromLinkId);
            act0.setCoord(network.getLinks().get(fromLinkId).getCoord());
            act0.setEndTime(rnd.nextInt(86400));
            Leg leg = populationFactory.createLeg("freight");
            Activity act1 = populationFactory.createActivityFromLinkId("freight_end", toLinkId);
            act1.setCoord(network.getLinks().get(toLinkId).getCoord());

            plan.addActivity(act0);
            plan.addLeg(leg);
            plan.addActivity(act1);
            freightPerson.addPlan(plan);
            population.addPerson(freightPerson);

            generated += 1;
            totalGeneratedPersons.increment();
        }
    }
}
