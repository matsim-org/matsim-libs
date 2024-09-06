package org.matsim.application.analysis.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.*;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.matsim.application.ApplicationUtils.globFile;
import static org.matsim.application.ApplicationUtils.loadScenario;

/**
 * Based on old api and analysis. Consider using {@link org.matsim.application.analysis.traffic.traveltime.SampleValidationRoutes}.
 */
@CommandLine.Command(
        name = "travel-time",
        description = "Run travel time analysis on events file. pre-analysis: analyze the empty network to validate the free flow speed of the network." +
                " post-analysis (default): validate the travel time and distance of executed trips." +
                " Transit mode of MATSim trips and corresponding mode in API need to be configured separately, due to different naming conventions."
)
@Deprecated
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

    @CommandLine.Option(names = "--api", description = "API to use for validation.", defaultValue = "HERE", required = true, showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    private API api;

    @CommandLine.Option(names = "--api-key", description = "API key. You can apply for free API key on their website", required = false)
    private String appCode;

    @CommandLine.Option(names = "--network", description = "Path to network file if API=NETWORK_FILE ", required = false)
    private Path timeVariantNetwork;

    @CommandLine.Option(names = "--network-change-events", description = "Path to network change events for time-variant network ", required = false)
    private Path networkChangeEvents;

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

    @CommandLine.Option(names = "--trip-mode", defaultValue = TransportMode.car, description = "Transit mode of MATSim trips to validate")
    private String tripMode;

    @CommandLine.Option(names = "--api-mode", description = "Transit mode to use in api (car if not given). Please refer to API doc for details.", required = false)
    private String apiMode;

    @CommandLine.Option(names = "--write-details", description = "Write JSON file for each calculated route")
    private boolean writeDetails;

    @CommandLine.Mixin
    private CrsOptions crs = new CrsOptions();

    @CommandLine.Mixin
    private ShpOptions shp = new ShpOptions();

    enum API {
        HERE, GOOGLE_MAP, NETWORK_FILE
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

        if ((api == API.HERE || api == API.GOOGLE_MAP) && appCode == null) {
            log.error("API key is required when using {}", api);
            return 2;
        } else if (api == API.NETWORK_FILE && timeVariantNetwork == null) {
            log.error("Network must be give when using {}", api);
            return 2;
        }

        // Set date to next Wednesday if not configured
        if (date == null) {

            // Google API only allows days in the future
            if (api == API.GOOGLE_MAP)
                date = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
            else
                date = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.WEDNESDAY));
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
            Predicate<NetworkTrip> tripFilter = carTrip -> true;
            if (shp.getShapeFile() != null) {
                ShpOptions.Index index = shp.createIndex(crs.getInputCRS(), "__");
                if (withInShp)
                    tripFilter = carTrip -> index.contains(carTrip.getArrivalLocation()) && index.contains(carTrip.getDepartureLocation());
                else
                    tripFilter = carTrip -> index.contains(carTrip.getArrivalLocation()) || index.contains(carTrip.getDepartureLocation());
            }
            log.info("Removed {} agents not selecting their best plan", size - populationIds.size());

            log.info("Running analysis for {} trips at {} on file {}", trips, date, events);

            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);

            TravelTimeDistanceValidator validator;
            if (api == API.HERE) {
                validator = new HereMapsRouteValidator(outputFolder, apiMode, appCode, date.toString(), transformation, writeDetails);
            } else if (api == API.GOOGLE_MAP) {
                validator = new GoogleMapRouteValidator(outputFolder, apiMode, appCode, date.toString(), transformation);
            } else if (api == API.NETWORK_FILE) {
                NetworkConfigGroup configGroup = new NetworkConfigGroup();

                Network network;
                if (networkChangeEvents != null) {
                    configGroup.setChangeEventsInputFile(networkChangeEvents.toString());
                    configGroup.setTimeVariantNetwork(true);

                    network = NetworkUtils.readNetwork(timeVariantNetwork.toString(), configGroup);

                    log.info("Using time variant network {}", networkChangeEvents);

                    List<NetworkChangeEvent> changeEvents = new ArrayList<>() ;
                    NetworkChangeEventsParser parser = new NetworkChangeEventsParser(network,changeEvents);
                    parser.readFile(networkChangeEvents.toString());

                    NetworkUtils.setNetworkChangeEvents(network, changeEvents);
                } else
                    network = NetworkUtils.readNetwork(timeVariantNetwork.toString(), configGroup);

                validator = new NetworkRouteValidator(network, apiMode);
            } else {
                throw new RuntimeException("Please enter the api correctly. Please choose from [here, google-map]");
            }
            Tuple<Double, Double> timeWindow = new Tuple<>((double) 0, (double) 3600 * 24);

            if (timeFrom != null && timeTo != null) {
                timeWindow = new Tuple<>(timeFrom, timeTo);
            }

            Files.createDirectories(Path.of(outputFolder));

            TravelTimeValidationRunner runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events.toString(), outputFolder, tripMode, validator, trips, timeWindow, tripFilter);
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
            Network network = NetworkUtils.readNetwork(networkPath.toString());
            CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(crs.getInputCRS(), TransformationFactory.WGS84);
            TravelTimeDistanceValidator validator;
            if (api == API.HERE) {
                validator = new HereMapsRouteValidator(outputFolder, apiMode, appCode, date.toString(), transformation, writeDetails);
            } else if (api == API.GOOGLE_MAP) {
                validator = new GoogleMapRouteValidator(outputFolder, apiMode, appCode, date.toString(), transformation);
            } else if (api == API.NETWORK_FILE) {
                NetworkConfigGroup configGroup = new NetworkConfigGroup();
                configGroup.setTimeVariantNetwork(true);
                validator = new NetworkRouteValidator(NetworkUtils.readNetwork(timeVariantNetwork.toString(), configGroup), apiMode);
            } else {
                throw new RuntimeException("Please enter the api correctly. Please choose from [here, google-map]");
            }

            TravelTimeValidationRunnerPreAnalysis preAnalysisRunner = new TravelTimeValidationRunnerPreAnalysis(network, trips, outputFolder, tripMode, validator, null);
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
