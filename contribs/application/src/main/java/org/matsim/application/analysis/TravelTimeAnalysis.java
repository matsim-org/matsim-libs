package org.matsim.application.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.vsp.traveltimedistance.CarTrip;
import org.matsim.contrib.analysis.vsp.traveltimedistance.HereMapsRouteValidator;
import org.matsim.contrib.analysis.vsp.traveltimedistance.TravelTimeValidationRunner;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

@CommandLine.Command(
        name = "travel-time",
        description = "Run travel time analysis on events file."
)
public class TravelTimeAnalysis implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(TravelTimeAnalysis.class);

    @CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Input run directory")
    private Path runDirectory;

    @CommandLine.Option(names = "--run-id", defaultValue = "*", description = "Pattern used to match runId", required = true)
    private String runId;

    @CommandLine.Option(names = "--output", defaultValue = "travelTimeResults", description = "Name of output folder", required = true)
    private String output;

    @CommandLine.Option(names = "--api-key", description = "HERE Maps API key, see here.com", required = true)
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
    private final CrsOptions crs = new CrsOptions();

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

    public static void main(String[] args) {
        System.exit(new CommandLine(new TravelTimeAnalysis()).execute(args));
    }

    @Override
    public Integer call() throws Exception {

        if (crs.getInputCRS() == null) {
            log.error("Input CRS is null. Please specify the input CRS");
            return 2;
        }

        Scenario scenario = AnalysisSummary.loadScenario(runId, runDirectory, crs);
        Path events = AnalysisSummary.glob(runDirectory, runId + ".*events.*", false)
                .orElseThrow(() -> new IllegalArgumentException("Could not find events file."));

        var scenario = ScenarioUtils.loadScenario()

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

        String outputFolder = runDirectory.resolve(output).toString();
        HereMapsRouteValidator validator = new HereMapsRouteValidator(outputFolder, appCode, date.toString(), transformation);
        validator.setWriteDetailedFiles(writeDetails);

		Tuple<Double, Double> timeWindow = new Tuple<>((double) 0, (double) 3600 * 30);
		if (timeFrom != null && timeTo != null) {
			timeWindow = new Tuple<>(timeFrom, timeTo);
		}

		TravelTimeValidationRunner runner = new TravelTimeValidationRunner(scenario.getNetwork(), populationIds, events.toString(), outputFolder, validator, trips, timeWindow, tripFilter);

        runner.run();

        return 0;
    }
}
