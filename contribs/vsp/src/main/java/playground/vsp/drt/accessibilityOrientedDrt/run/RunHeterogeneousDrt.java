package playground.vsp.drt.accessibilityOrientedDrt.run;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.drt.analysis.afterSimAnalysis.DrtVehicleStoppingTaskWriter;
import org.matsim.contrib.drt.extension.preplanned.optimizer.WaitForStopTask;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.collections.Tuple;
import picocli.CommandLine;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.HeteogeneousRequestValidator;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.PassengerAttribute;
import playground.vsp.drt.accessibilityOrientedDrt.optimizer.PersonAttributeBasedConstraintSelector;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static playground.vsp.drt.accessibilityOrientedDrt.alternativeMode.AlternativeModeTripData.*;
import static playground.vsp.drt.accessibilityOrientedDrt.optimizer.PassengerAttribute.PREMIUM;

public class RunHeterogeneousDrt implements MATSimAppCommand {
    @CommandLine.Option(names = "--config", description = "path to config file", required = true)
    private String configPath;

    @CommandLine.Option(names = "--output", description = "output root directory", required = true)
    private String outputRootDirectory;

    @CommandLine.Option(names = "--fleet-sizing", description = "a triplet: [from max interval]. ", arity = "1..*", defaultValue = "300 600 10")
    private List<Integer> fleetSizing;

    @CommandLine.Option(names = "--iterations", description = "outer iterations", defaultValue = "20")
    private int outerIterations;

    @CommandLine.Option(names = "--learning-rate", description = "learning rate with exp discount", defaultValue = "0.5")
    private double learningRate;

    @CommandLine.Option(names = "--time-bin-size", description = "time bin size for the travel time analysis", defaultValue = "900")
    private int timeBinSize;

    @CommandLine.Option(names = "--alternative-data", description = "path to alternative mode data", required = true)
    private Path alternativeDataPath;

    private static final Logger log = LogManager.getLogger(RunHeterogeneousDrt.class);

    private final Map<Integer, Double> thresholdMap = new LinkedHashMap<>();
    private final Map<String, String> personAttributeMap = new LinkedHashMap<>();

    // Tuple: departure time, trip duration ratio (with respect to max drt total travel time)
    private final Map<String, Tuple<Double, Double>> alternativeModeData = new HashMap<>();

    public static void main(String[] args) {
        new RunHeterogeneousDrt().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        // Decoding fleet sizing sequence
        Preconditions.checkArgument(fleetSizing.size() == 3);
        int fleetFrom = fleetSizing.get(0);
        int fleetMax = fleetSizing.get(1);
        int fleetInterval = fleetSizing.get(2);

        Config configForGettingData = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());

        // Reading person attributes
        Population inputPlans = PopulationUtils.readPopulation(Path.of(configPath).getParent().toString() + "/" + configForGettingData.plans().getInputFile());
        for (Person person : inputPlans.getPersons().values()) {
            personAttributeMap.put(person.getId().toString(), person.getAttributes().getAttribute(PassengerAttribute.ATTRIBUTE_NAME).toString());
        }

        // Initialize the threshold map
        double simulationEndTime = configForGettingData.qsim().getEndTime().orElse(3600 * 30);
        for (int i = 0; i < simulationEndTime; i += timeBinSize) {
            thresholdMap.put(i, 0.0);
        }

        // Read alternative mode data
        log.info("Reading alternative mode data...");
        DrtConfigGroup drtConfigGroupForGettingData = DrtConfigGroup.getSingleModeDrtConfig(configForGettingData);
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(alternativeDataPath),
                CSVFormat.TDF.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser.getRecords()) {
                String personId = record.get("id");
                double departureTime = Double.parseDouble(record.get(DEPARTURE_TIME));
                double alternativeTravelTime = Double.parseDouble(record.get(ACTUAL_TOTAL_TRAVEL_TIME));
                double directTravelTime = Double.parseDouble(record.get(DIRECT_CAR_TRAVEL_TIME));
                DrtOptimizationConstraintsSetImpl constraints = drtConfigGroupForGettingData.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
                double ratio = alternativeTravelTime / (constraints.getMaxTravelTimeAlpha() * directTravelTime + constraints.getMaxTravelTimeBeta());
                alternativeModeData.put(personId, new Tuple<>(departureTime, ratio));
            }
        }

//        PerformanceAnalysis overallAnalysis = new PerformanceAnalysis(drtConfigGroupForGettingData, alternativeDataPath.toString(), outputRootDirectory + "/overall-summary.tsv");
//        overallAnalysis.writeTitle();

        for (int fleetSize = fleetFrom; fleetSize <= fleetMax; fleetSize += fleetInterval) {
            String fleetSizeFolder = outputRootDirectory + "/" + fleetSize + "-veh";
//            PerformanceAnalysis singleCaseAnalysis = new PerformanceAnalysis
//                    (drtConfigGroupForGettingData, alternativeDataPath.toString(), fleetSizeFolder + "/iterations-summary.tsv");
//            singleCaseAnalysis.writeTitle();

            // Start outer iterations
            for (int i = 0; i <= outerIterations; i++) {
                String outputFolder = fleetSizeFolder + "/iter-" + i;

                Config config = ConfigUtils.loadConfig(configPath, new MultiModeDrtConfigGroup(), new DvrpConfigGroup());
                config.controller().setOutputDirectory(outputFolder);

                // Currently we only focus on single DRT mode
                DrtConfigGroup drtConfigGroup = DrtConfigGroup.getSingleModeDrtConfig(config);
                drtConfigGroup.setVehiclesFile("./vehicles/" + fleetSize + "-8_seater-drt-vehicles.xml");
                PersonAttributeBasedConstraintSelector.prepareDrtConstraint(drtConfigGroup,
                        1.5, 900, 600,
                        1.2, 450, 300);

                Controler controler = DrtControlerCreator.createControler(config, false);

                // Adding the New Request Validator
                controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(drtConfigGroup.getMode()) {
                    @Override
                    protected void configureQSim() {
                        bindModal(PassengerRequestValidator.class).toProvider(
                                modalProvider(getter -> new HeteogeneousRequestValidator(getter.get(Population.class),
                                        thresholdMap, timeBinSize, alternativeModeData))).asEagerSingleton();
                    }
                });
                // Adding person attribute-based constraint selector
                controler.addOverridingModule(new AbstractDvrpModeModule(drtConfigGroup.getMode()) {
                    @Override
                    public void install() {
                        bindModal(ConstraintSetChooser.class).toProvider(
                                () -> new PersonAttributeBasedConstraintSelector(drtConfigGroup)).in(Singleton.class);
                    }
                });

                controler.run();

                // Plot DRT stopping tasks
                new DrtVehicleStoppingTaskWriter(Path.of(outputFolder)).
                        addingCustomizedTaskToAnalyze(WaitForStopTask.TYPE).run(WaitForStopTask.TYPE);

//                // Analyze KPI
//                // TODO improve analysis so that it can distinguish different types of users
//                singleCaseAnalysis.writeDataEntry(outputFolder, fleetSize);
//
//                // Update tem population
//                if (i != outerIterations) {
//                    // Analyze
//                    adjustTimeVaryingThreshold(outputFolder);
//                } else {
//                    // Write overall analysis
//                    overallAnalysis.writeDataEntry(outputFolder, fleetSize);
//                }
            }
        }
        return 0;
    }

    private void adjustTimeVaryingThreshold(String outputFolder) throws IOException {
        log.info("Processing plans...");
        // Initialization
        Map<Integer, List<Double>> tripLengthRatiosPerTimeBinMap = new HashMap<>();

        // Read output trips
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(outputFolder + "/output_drt_legs_drt.csv")),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser.getRecords()) {
                // We do not consider premium trips when calculating the threshold map
                String personAttribute = personAttributeMap.get(record.get("personId"));
                if (personAttribute.equals(PREMIUM)){
                    continue;
                }

                double departureTime = Double.parseDouble(record.get("departureTime"));
                double arrivalTime = Double.parseDouble(record.get("arrivalTime"));
                double latestArrivalTime = Double.parseDouble(record.get("latestArrivalTime"));

                double actualTravelTime = arrivalTime - departureTime;
                double maxTravelTime = latestArrivalTime - departureTime;
                double ratio = actualTravelTime / maxTravelTime;

                int timeBin = (int) Math.floor(departureTime / timeBinSize) * timeBinSize;
                tripLengthRatiosPerTimeBinMap.computeIfAbsent(timeBin, t -> new ArrayList<>()).add(ratio);
            }
        }

        // Update the threshold map
        for (int timeBin : tripLengthRatiosPerTimeBinMap.keySet()) {
            double averageTripLengthRatio =
                    tripLengthRatiosPerTimeBinMap.get(timeBin).stream().mapToDouble(d -> d).average().orElseThrow();
            double previousValue = thresholdMap.get(timeBin);
            double updatedValue = learningRate * averageTripLengthRatio + (1 - learningRate) * previousValue;
            updatedValue = Math.min(1.0, updatedValue);
            thresholdMap.put(timeBin, updatedValue);
        }

        // Write down the current threshold map
        CSVPrinter printer = new CSVPrinter(new FileWriter(outputFolder + "/time-varying-threshold-map.tsv"), CSVFormat.TDF);
        printer.printRecord("time", "threshold");
        for (int timeBin : thresholdMap.keySet()) {
            printer.printRecord(timeBin, thresholdMap.get(timeBin));
        }
        printer.close();
    }
}
