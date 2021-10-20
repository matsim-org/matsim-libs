package org.matsim.application.automatedCalibration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AutomaticScenarioCalibrator {
    /**
     * The maximum allowed running time in seconds
     */
    private final long maxRunningTime;
    /**
     * Stop the auto-tuning after certain amount of non-improving run
     */
    private final int patience;
    /**
     * The auto-tuning process will stop when all the error (normalized to total number of trips) are within the allowed range
     */
    private final double targetError;
    protected final String configFile;

    private static final Logger log = LogManager.getLogger(AutomaticScenarioCalibrator.class);
    private final Map<String, Map<Double, Double>> errorMap = new HashMap<>();
    private final Map<String, Map<Double, Double>> referenceMap = new HashMap<>();
    protected int counter = 0;
    private int nonImprovingRuns = 0;
    private double maxAbsError = 1.0;
    private double currentMaxAbsError = 0.0;
    private double currentSumAbsError = 0.0;
    private boolean complete = false;
    private long startTime;
    protected final Config config;
    private final ParameterTuner parameterTuner;
    private final String relevantPersonsFile;
    protected final String outputFolder;

    public AutomaticScenarioCalibrator(String configFile, String output, String referenceDataFile, double targetError, long maxRunningTime, int patience, String relevantPersonsFile) throws IOException {
        this.targetError = targetError;
        this.maxRunningTime = maxRunningTime;
        this.patience = patience;
        this.configFile = configFile;
        this.config = ConfigUtils.loadConfig(configFile);
        this.parameterTuner = new SimpleParameterTuner(targetError);
        this.relevantPersonsFile = relevantPersonsFile;
        readReferenceData(referenceDataFile);
        initializeErrorMap();
        if (output == null || output.equals("")) {
            output = "./output/auto-calibration";
        }
        this.outputFolder = output;
    }

    public void calibrate() throws IOException {
        startTime = System.currentTimeMillis();
        config.controler().setOutputDirectory(outputFolder + "/run-0");
        // Auto tuning loop
        while (true) {
            runSimulation();
            analyzeResults();
            writeRecord();
            checkIfComplete();
            if (complete) {
                break;
            }
            counter++;
            modifyConfig();
        }
    }

    /**
     * This function is to be implemented in the different scenario. Important: Please do not build the scenario (and
     * the controller) with the original config! Create (load) a new config in each simulation run instead!
     */
    public abstract void runSimulation();

    private void analyzeResults() throws IOException {
        // Get output plan  //TODO may be improved?
        String runId = config.controler().getRunId();
        String outputPlansFileName = "/output_plans.xml.gz";
        if (runId != null && !runId.equals("")) {
            outputPlansFileName = "/" + runId + ".output_plans.xml.gz";
        }
        Population outputPlans = PopulationUtils.readPopulation(config.controler().getOutputDirectory() + outputPlansFileName);
        // Analyze mode share of the output plan
        Map<String, Map<Double, Double>> modeShare = analyzeModeShare(outputPlans);
        // update error map
        updateErrorMap(modeShare);
    }

    private void writeRecord() throws IOException {
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/record.csv", true), CSVFormat.DEFAULT);
        csvWriter.printRecord("mode", "asc", "marginal");
        csvWriter.printRecord("Tuning run " + counter, "Max abs error = " + currentMaxAbsError,
                "Sum abs error = " + currentSumAbsError);
        csvWriter.printRecord(TransportMode.car,
                config.planCalcScore().getModes().get(TransportMode.car).getConstant(),
                config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
        csvWriter.printRecord(TransportMode.ride,
                config.planCalcScore().getModes().get(TransportMode.ride).getConstant(),
                config.planCalcScore().getModes().get(TransportMode.ride).getMarginalUtilityOfTraveling());
        csvWriter.printRecord(TransportMode.pt,
                config.planCalcScore().getModes().get(TransportMode.pt).getConstant(),
                config.planCalcScore().getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling());
        csvWriter.printRecord(TransportMode.bike,
                config.planCalcScore().getModes().get(TransportMode.bike).getConstant(),
                config.planCalcScore().getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling());
        csvWriter.printRecord(TransportMode.walk,
                config.planCalcScore().getModes().get(TransportMode.walk).getConstant(),
                config.planCalcScore().getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling());
        csvWriter.close();
    }

    private void checkIfComplete() {
        // Case 1: Everything is within range
        if (maxAbsError < targetError) {
            complete = true;
            log.info("Auto-tuning process ends successfully with all error within range!");
        }

        // Case 2: No improvements after certain number of runs
        if (nonImprovingRuns >= patience) {
            complete = true;
            log.warn("Auto-tuning terminates because the results have not improved for " + patience + " cycles!");
        }

        // Case 3: Overtime
        long runningTime = (System.currentTimeMillis() - startTime) / 1000;
        if (runningTime > maxRunningTime) {
            complete = true;
            log.warn("Auto-tuning terminates because the maximum time allowed is exceeded!");
        }
    }

    private void modifyConfig() {
        // Tune parameter
        parameterTuner.tune(config, errorMap);
        // update output folder for next run
        config.controler().setOutputDirectory(outputFolder + "/run-" + counter);
    }

    // Initializations
    private void readReferenceData(String referenceDataFile) throws IOException {
        // For a sample reference data, please see the svn //TODO
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(referenceDataFile)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            List<Double> distances = new ArrayList<>();
            distances.add(-1.0); // add a placeholder
            for (int i = 1; i < parser.getHeaderNames().size(); i++) {
                distances.add(Double.parseDouble(parser.getHeaderNames().get(i)));
            }
            for (CSVRecord record : parser) {
                String mode = record.get(0);
                referenceMap.put(mode, new HashMap<>());
                for (int i = 1; i < record.size(); i++) {
                    referenceMap.get(mode).put(distances.get(i), Double.parseDouble(record.get(i)));
                }
            }
        }
    }

    private Map<String, Map<Double, MutableDouble>> initializeModeCount() {
        Map<String, Map<Double, MutableDouble>> modeCount = new HashMap<>();
        modeCount.put(TransportMode.car, new HashMap<>());
        modeCount.put(TransportMode.ride, new HashMap<>());
        modeCount.put(TransportMode.pt, new HashMap<>());
        modeCount.put(TransportMode.bike, new HashMap<>());
        modeCount.put(TransportMode.walk, new HashMap<>());
        return modeCount;
    }

    private void initializeErrorMap() {
        errorMap.put(TransportMode.car, new HashMap<>());
        errorMap.put(TransportMode.ride, new HashMap<>());
        errorMap.put(TransportMode.pt, new HashMap<>());
        errorMap.put(TransportMode.bike, new HashMap<>());
        errorMap.put(TransportMode.walk, new HashMap<>());
    }

    private Map<String, Map<Double, Double>> analyzeModeShare(Population outputPlans) throws IOException {
        Map<String, Map<Double, MutableDouble>> modeCount = initializeModeCount();
        Map<String, Map<Double, Double>> modeShare = new HashMap<>();
        int totalTrips = 0;
        MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();

        // Get relevant persons
        List<Id<Person>> relevantPersons = new ArrayList<>();
        if (relevantPersonsFile != null && !relevantPersonsFile.equals("")) {
            // read persons live in the area
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(relevantPersonsFile)),
                    CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser) {
                    relevantPersons.add(Id.createPersonId(record.get(0)));
                }
            }
        }

        for (Person person : outputPlans.getPersons().values()) {
            if (relevantPersons.contains(person.getId()) || relevantPersons.isEmpty()) {
                Plan plan = person.getSelectedPlan();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip : trips) {
                    String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                    double euclideanDistance = CoordUtils.calcEuclideanDistance
                            (trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
                    addLegToModeCount(mode, euclideanDistance, modeCount);
                    totalTrips++;
                }
            }
        }

        for (String mode : modeCount.keySet()) {
            for (Double distance : modeCount.get(mode).keySet()) {
                double share = modeCount.get(mode).get(distance).doubleValue() / totalTrips;
                modeShare.computeIfAbsent(mode, m -> new HashMap<>()).put(distance, share);
            }
        }
        return modeShare;
    }

    /**
     * Designed for the distance grouping: 0-1, 1-2, 2-5, 5-10, 10-20, 20+
     * For other distance groupings, please override this function
     */
    private void addLegToModeCount(String mode, double euclideanDistance, Map<String, Map<Double, MutableDouble>> modeCount) {
        if (euclideanDistance < 1) {
            modeCount.get(mode).computeIfAbsent(0.5, c -> new MutableDouble()).increment();
        } else if (euclideanDistance < 2) {
            modeCount.get(mode).computeIfAbsent(1.5, c -> new MutableDouble()).increment();
        } else if (euclideanDistance < 5) {
            modeCount.get(mode).computeIfAbsent(3.5, c -> new MutableDouble()).increment();
        } else if (euclideanDistance < 10) {
            modeCount.get(mode).computeIfAbsent(7.5, c -> new MutableDouble()).increment();
        } else if (euclideanDistance < 20) {
            modeCount.get(mode).computeIfAbsent(15.0, c -> new MutableDouble()).increment();
        } else {
            modeCount.get(mode).computeIfAbsent(35.0, c -> new MutableDouble()).increment();
        }
    }

    private void updateErrorMap(Map<String, Map<Double, Double>> modeShare) {
        currentMaxAbsError = 0.0;
        currentSumAbsError = 0.0;
        for (String mode : referenceMap.keySet()) {
            for (double distance : referenceMap.get(mode).keySet()) {
                double error = modeShare.getOrDefault(mode, new HashMap<>()).getOrDefault(distance, 0.0) - referenceMap.get(mode).get(distance);
                errorMap.get(mode).put(distance, error);
                currentSumAbsError += Math.abs(error);
                if (Math.abs(error) > currentMaxAbsError) {
                    currentMaxAbsError = Math.abs(error);
                }
            }
        }
        if (currentMaxAbsError >= maxAbsError) {
            // No improvements for this run
            nonImprovingRuns++;
        } else {
            // Results have been improved
            nonImprovingRuns = 0;
            maxAbsError = currentMaxAbsError;
        }
    }
}
