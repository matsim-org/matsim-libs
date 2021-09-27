package org.matsim.application.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTrip;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.contrib.analysis.vsp.traveltimedistance.TravelTimeValidationRunner;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.ApplicationUtils.loadScenario;

@CommandLine.Command(
        name = "travel-time",
        description = "Run travel time analysis on events file. pre-analysis: analyze the empty network to validate the free flow speed of the network. post-analysis (default): validate the travel time and distance of executed trips"
)
public class TravelTimeAnalysis implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(TravelTimeAnalysis.class);

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Input run directory")
    private Path runDirectory;

    @CommandLine.Option(names = "--analysis-type", defaultValue = "post-analysis", description = "type of the analysis. Choose from [pre-analysis, post-analysis]", required = true)
    private String type;

    @CommandLine.Option(names = "--run-id", defaultValue = "*", description = "Pattern used to match runId", required = true)
    private String runId;

    @CommandLine.Option(names = "--output", defaultValue = "travelTimeResults", description = "Name of output folder", required = true)
    private String output;

    @CommandLine.Option(names = "--api", description = "Online API used. Choose from [here, google-map (todo)]", defaultValue = "here", required = true)
    private String api;

    @CommandLine.Option(names = "--api-key", description = "API key. You can apply for free API key on their website", required = true)
    private String appCode;

    @CommandLine.Option(names = "--date", description = "The date to validate travel times for, format: YYYY-MM-DD")
    private LocalDate date;

    @CommandLine.Option(names = "--trips", description = "The number of trips to validate", defaultValue = "500")
    private int trips;

    @CommandLine.Option(names = "--within-shp", description = "Only consider trips that have start and end within provided shp", defaultValue = "false")
    private boolean withInShp;

    @CommandLine.Option(names = "--from", defaultValue = "0", description = "From time window in seconds")
    private Double timeFrom;

    @CommandLine.Option(names = "--to", defaultValue = "86400", description = "To time window in seconds")
    private Double timeTo;

    @CommandLine.Option(names = "--write-details", description = "Write JSON file for each calculated route")
    private boolean writeDetails;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    private final Random rnd = new Random(1234);

    public static void main(String[] args) {
        System.exit(new CommandLine(new TravelTimeAnalysis()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        if (crs.getInputCRS() == null) {
            log.error("Input CRS is null. Please specify the input CRS");
            return 2;
        }

        String outputFolder = runDirectory.resolve(output).toString();
        if (type.equals("post-analysis")){
            Path events = globFile(runDirectory, runId + ".*events.*");
            Scenario scenario = loadScenario(runId, runDirectory, crs);
            Set<Id<Person>> populationIds = scenario.getPopulation().getPersons().keySet();
            BestPlanSelector<Plan, Person> selector = new BestPlanSelector<>();
            int size = populationIds.size();
            populationIds.removeIf(p -> {
                Person person = scenario.getPopulation().getPersons().get(p);
                return selector.selectPlan(person) != person.getSelectedPlan();
            });
            Predicate<CarTrip> tripFilter = carTrip -> true;
            if (shp.getShapeFile() != null) {
                ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "__");
                if (withInShp)
                    tripFilter = carTrip -> index.contains(carTrip.getArrivalLocation()) && index.contains(carTrip.getDepartureLocation());
                else
                    tripFilter = carTrip -> index.contains(carTrip.getArrivalLocation()) || index.contains(carTrip.getDepartureLocation());
            }
            log.info("Removed {} agents not selecting their best plan", size - populationIds.size());

            if (date == null)
                date = LocalDate.now();

            log.info("Running analysis for {} trips at {} on file {}", trips, date, events);

            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);


            HereMapsRouteValidator validator = new HereMapsRouteValidator(outputFolder, appCode, date.toString(), transformation);
            validator.setWriteDetailedFiles(writeDetails);

            Tuple<Double, Double> timeWindow = new Tuple<>((double) 0, (double) 3600 * 30);
            if (timeFrom != null && timeTo != null) {
                timeWindow = new Tuple<>(timeFrom, timeTo);
            }

            TravelTimeValidationRunner runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events.toString(), outputFolder, validator, trips, timeWindow, tripFilter);

            runner.run();

            return 0;

        } else if (type.equals("pre-analysis")){
            Path networkPath = globFile(runDirectory, ".*network.*");
            if (!Files.exists(networkPath)) {
                log.error("Network file does not exist. Please make sure the network file is in the run directory");
                return 2;
            }
            Network network = NetworkUtils.readNetwork(networkPath.toString());
            List<Link> links = network.getLinks().values().stream().
                    filter(l -> l.getAllowedModes().contains(TransportMode.car)).
                    collect(Collectors.toList());
            int numOfLinks = links.size();

            // Create router
            FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
            LeastCostPathCalculatorFactory fastAStarLandmarksFactory = new SpeedyALTFactory();
            OnlyTimeDependentTravelDisutilityFactory disutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
            TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
            LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
                    travelTime);

            // Read shapefile if presents
            List<Link> linksInsideShp = new ArrayList<>();
            List<Link> outsideLinks = new ArrayList<>();
            if (shp.getShapeFile() != null) {
                Geometry geometry = shp.getGeometry();
                for (Link link : links) {
                    if (MGC.coord2Point(link.getToNode().getCoord()).within(geometry)) {
                        linksInsideShp.add(link);
                    }
                }
                outsideLinks.addAll(links);
                outsideLinks.removeAll(linksInsideShp);
            }

            // Choose random trips to validate
            CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/results.csv"), CSVFormat.DEFAULT);
            csvWriter.printRecord("trip_number", "trip_category", "from_x", "from_y", "to_x", "to_y", "simulated_travel_time", "validated_travel_time");
            int counter = 0;
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);
            HereMapsRouteValidator validator = new HereMapsRouteValidator(outputFolder.toString(), appCode, "2021-01-01", transformation);
            validator.setWriteDetailedFiles(writeDetails);

            Link fromLink;
            Link toLink;
            String tripType;
            while (counter < trips) {
                if (!linksInsideShp.isEmpty()) {
                    int numOfLinksInsideShp = linksInsideShp.size();
                    int numOfOutsideLinks = outsideLinks.size();

                    if (counter < 0.6 * trips) {
                        fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                        toLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                        tripType = "inside";
                    } else if (counter < 0.9 * trips) {
                        fromLink = linksInsideShp.get(rnd.nextInt(numOfLinksInsideShp));
                        toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                        tripType = "cross-border";
                    } else {
                        fromLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                        toLink = outsideLinks.get(rnd.nextInt(numOfOutsideLinks));
                        tripType = "outside";
                    }
                } else {
                    fromLink = links.get(rnd.nextInt(numOfLinks));
                    toLink = links.get(rnd.nextInt(numOfLinks));
                    tripType = "unknown";
                }

                if (!fromLink.getToNode().getId().equals(toLink.getToNode().getId())) {
                    String detailedFile = outputFolder + "/detailed-record/trip" + counter + ".json.gz";
                    Coord fromCorrd = fromLink.getToNode().getCoord();
                    Coord toCoord = toLink.getToNode().getCoord();
                    double validatedTravelTime = validator.getTravelTime
                            (fromCorrd, toCoord, 1, detailedFile).getFirst();
                    if (validatedTravelTime < 60){
                        continue;
                    }
                    double simulatedTravelTime = router.calcLeastCostPath
                            (fromLink.getToNode(), toLink.getToNode(), 0, null, null).travelTime;
                    csvWriter.printRecord(Integer.toString(counter), tripType, Double.toString(fromCorrd.getX()),
                            Double.toString(fromCorrd.getY()), Double.toString(toCoord.getX()),
                            Double.toString(toCoord.getY()), Double.toString(simulatedTravelTime),
                            Double.toString(validatedTravelTime));
                    counter++;
                }
            }
            csvWriter.close();
            return 0;
        }

        log.error("Please enter the correct analysis type: [pre-analysis, post-analysis]");
        return  2;
    }


}
