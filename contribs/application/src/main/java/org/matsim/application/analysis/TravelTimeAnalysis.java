package org.matsim.application.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.*;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

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

    @CommandLine.Option(names = "--analysis-type", defaultValue = "POST_ANALYSIS", description = "type of the analysis. Choose from [PRE_ANALYSIS, POST_ANALYSIS]", required = true)
    private AnalysisTypes type;

    @CommandLine.Option(names = "--run-id", defaultValue = "*", description = "Pattern used to match runId", required = true)
    private String runId;

    @CommandLine.Option(names = "--output", defaultValue = "travelTimeValidation", description = "Name of output folder", required = true)
    private String output;

    @CommandLine.Option(names = "--api", description = "Online API used. Choose from [HERE, GOOGLE_MAP]", defaultValue = "HERE", required = true)
    private TravelTimeDistanceValidators api;

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

    enum TravelTimeDistanceValidators {
        HERE, GOOGLE_MAP
    }

    enum AnalysisTypes {
        PRE_ANALYSIS, POST_ANALYSIS
    }

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
        if (type == AnalysisTypes.POST_ANALYSIS) {
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

            TravelTimeDistanceValidator validator;
            if (api == TravelTimeDistanceValidators.HERE) {
                validator = new HereMapsRouteValidator(outputFolder, appCode, date.toString(), transformation, writeDetails);
            } else if (api == TravelTimeDistanceValidators.GOOGLE_MAP) {
                validator = new GoogleMapRouteValidator(outputFolder, appCode, date.toString(), transformation, writeDetails);
            } else {
                throw new RuntimeException("Please enter the api correctly. Please choose from [here, google-map]");
            }
            Tuple<Double, Double> timeWindow = new Tuple<>((double) 0, (double) 3600 * 24);

            if (timeFrom != null && timeTo != null) {
                timeWindow = new Tuple<>(timeFrom, timeTo);
            }

            TravelTimeValidationRunner runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events.toString(), outputFolder, validator, trips, timeWindow, tripFilter);
            runner.run();

            return 0;

        } else if (type == AnalysisTypes.PRE_ANALYSIS) {
            Path networkPath = globFile(runDirectory, "*network*");
            log.info("Network file to be read: " + networkPath);
            if (!networkPath.toString().endsWith(".xml") && !networkPath.toString().endsWith(".xml.gz")) {
                log.error("There are other non-xml file with the name network in the folder. Please consider change the run directory and only keep the correct network xml file in the run directory");
                return 2;
            }
            if (!Files.exists(networkPath)) {
                log.error("Network file does not exist. Please make sure the network file is in the run directory");
                return 2;
            }
            Network network = NetworkUtils.readTimeInvariantNetwork(networkPath.toString());
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);
            TravelTimeDistanceValidator validator;
            if (api == TravelTimeDistanceValidators.HERE) {
                validator = new HereMapsRouteValidator(outputFolder, appCode, "2021-01-01", transformation, writeDetails);
            } else if (api == TravelTimeDistanceValidators.GOOGLE_MAP) {
                validator = new GoogleMapRouteValidator(outputFolder, appCode, date.toString(), transformation, writeDetails);
            } else {
                throw new RuntimeException("Please enter the api correctly. Please choose from [here, google-map]");
            }

            TravelTimeValidationRunnerPreAnalysis preAnalysisRunner = new TravelTimeValidationRunnerPreAnalysis(network, trips, outputFolder, validator, null);
            if (shp.getShapeFile() != null) {
                preAnalysisRunner.setKeyAreaGeometry(shp.getGeometry());
            }
            preAnalysisRunner.run();

            return 0;
        }

        log.error("Please enter the correct analysis type: [pre-analysis, post-analysis]");
        return 2;
    }


}
