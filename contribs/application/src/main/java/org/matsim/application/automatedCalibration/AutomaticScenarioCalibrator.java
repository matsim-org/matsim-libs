package org.matsim.application.automatedCalibration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.Time;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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
    /**
     * There are 2 stages in the automated tuning process:
     * Stage 1 (rough-tuning):
     * Always start from the original input plans, longer iteration process (e.g. 300 - 500) should be used.
     * Stage 2 (fine-tuning):
     * After the mode share distribution is not so far away from the reference data (e.g., +/- 5% for all modes),
     * we will start to use the output plans from previous run as the new starting point.
     * Meanwhile, the number of iterations (150) will also be reduced, as everything tends to be more stable.
     */
    private static boolean usePreviousOutput = false;

    protected final String configFile;

    private static final Logger log = LogManager.getLogger(AutomaticScenarioCalibrator.class);
    private final Map<String, Map<String, Double>> referenceMap = new HashMap<>();
    protected int counter = 0;
    private int nonImprovingRuns = 0;
    private double maxAbsError = 1.0;
    private double sumAbsError = Double.MAX_VALUE;
    private boolean complete = false;
    private long startTime;
    protected final Config config;
    private final ParameterTuner parameterTuner;
    private final List<Id<Person>> relevantPersons = new ArrayList<>();
    protected final String outputFolder;

    private final List<Trip> trips = new ArrayList<>();
    private final Map<String, Map<String, Double>> errorMap = new HashMap<>();
    private double currentMaxAbsError = 0.0;
    private double currentSumAbsError = 0.0;

    private final String[] modes = new String[]{TransportMode.car, TransportMode.ride, TransportMode.pt, TransportMode.bike, TransportMode.walk};
    private final DistanceGrouping distanceGrouping;

    public enum DistanceInterpretations {euclideanDistance, networkDistance}

    private final DistanceInterpretations distanceInterpretations;

    public AutomaticScenarioCalibrator(String configFile, String output, String referenceDataFile,
                                       double targetError, long maxRunningTime, int patience,
                                       String relevantPersonsFile, DistanceInterpretations distanceInterpretations) throws IOException {
        this.targetError = targetError;
        this.maxRunningTime = maxRunningTime;
        this.patience = patience;
        this.configFile = configFile;
        this.config = ConfigUtils.loadConfig(configFile);
        this.distanceGrouping = new StandardDistanceGrouping();
        this.parameterTuner = new SimpleParameterTuner(modes, distanceGrouping);
        if (output == null || output.equals("")) {
            output = "./output/auto-calibration";
        }
        this.outputFolder = output;
        this.distanceInterpretations = distanceInterpretations;

        readRelevantPersonsFile(relevantPersonsFile);
        readReferenceData(referenceDataFile);
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

    class Trip {
        private final String mode;
        private final double distance;
        private final double travelTime;

        Trip(String mode, double distance, double travelTime) {
            this.mode = mode;
            this.distance = distance;
            this.travelTime = travelTime;
        }

        public String getMode() {
            return mode;
        }

        public double getDistance() {
            return distance;
        }

        public double getTravelTime() {
            return travelTime;
        }
    }

    /**
     * This function is to be implemented in the different scenario. Important: Please do not build the scenario (and
     * the controller) with the original config! Create (load) a new config in each simulation run instead!
     */
    public abstract void runSimulation();


    /**
     * Analyze the actual mode share based on the output trip csv file. Then calculate the error between simulation
     * and reference data.
     */
    private void analyzeResults() throws IOException {
        readTrips();
        Map<String, Map<String, Double>> modeShare = analyzeModeShare();
        calculateError(modeShare);
    }

    private void readTrips() throws IOException {
        trips.clear();
        String runId = config.controler().getRunId();
        String outputTripsFileName = "/output_trips.csv.gz";
        if (runId != null && !runId.equals("")) {
            outputTripsFileName = "/" + runId + ".output_trips.csv.gz";
        }
        String outputTripsPath = config.controler().getOutputDirectory() + outputTripsFileName;
        String decompressedTripsPath = outputTripsPath.replace(".gz", "");

        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(Path.of(outputTripsPath).toFile()));
             FileOutputStream fos = new FileOutputStream(Path.of(decompressedTripsPath).toFile())) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(decompressedTripsPath)),
                CSVFormat.DEFAULT.withDelimiter(';').withFirstRecordAsHeader())) {
            for (CSVRecord record : parser.getRecords()) {
                Id<Person> personId = Id.createPersonId(record.get("person"));
                if (!relevantPersons.isEmpty() && !relevantPersons.contains(personId)) {
                    continue;
                }
                String mode = record.get("main_mode");
                String distanceName;
                switch (distanceInterpretations) {
                    case networkDistance:
                        distanceName = "traveled_distance";
                        break;
                    case euclideanDistance:
                        distanceName = "euclidean_distance";
                        break;
                    default:
                        throw new RuntimeException("Please use the the correct distance interpretation: " +
                                "[euclideanDistance, networkDistance]");
                }
                double distance = Double.parseDouble(record.get(distanceName));
                double travelTime = Time.parseTime(record.get("trav_time"));
                Trip trip = new Trip(mode, distance, travelTime);
                trips.add(trip);
            }
        }
    }

    private Map<String, Map<String, Double>> analyzeModeShare() {
        // Compute mode count map
        Map<String, Map<String, MutableInt>> modeCount = new HashMap<>();
        for (String mode : modes) {
            modeCount.put(mode, new HashMap<>());
        }
        int totalTrips = 0;

        for (Trip trip : trips) {
            String mode = trip.getMode();
            double distance = trip.getDistance();
            String distanceGroup = distanceGrouping.assignDistanceGroup(distance);
            modeCount.get(mode).computeIfAbsent(distanceGroup, c -> new MutableInt()).increment();
            totalTrips++;
        }

        // Calculate trip share
        Map<String, Map<String, Double>> modeShare = new HashMap<>();
        for (String mode : modeCount.keySet()) {
            for (String distanceGroup : modeCount.get(mode).keySet()) {
                double share = modeCount.get(mode).get(distanceGroup).doubleValue() / totalTrips;
                modeShare.computeIfAbsent(mode, m -> new HashMap<>()).put(distanceGroup, share);
            }
        }
        return modeShare;
    }

    /**
     * Write down record and performance of the current run
     */
    private void writeRecord() throws IOException {
        CSVPrinter csvWriter = new CSVPrinter(new FileWriter(outputFolder + "/record.csv", true), CSVFormat.DEFAULT);
        csvWriter.printRecord("Run " + counter + "(run-" + counter % (patience + 1) + ")", "ASC", "Marginal", "Mode share Error");
        for (String mode : modes) {
            csvWriter.printRecord(mode,
                    config.planCalcScore().getModes().get(mode).getConstant(),
                    config.planCalcScore().getModes().get(mode).getMarginalUtilityOfTraveling(),
                    Double.toString(errorMap.get(mode).values().stream().mapToDouble(v -> v).sum()));
        }
        csvWriter.printRecord("Summary", "Max abs err = " + currentMaxAbsError,
                "Sum abs err = " + currentSumAbsError, "Use previous output plans = " + usePreviousOutput);
        csvWriter.println();
        csvWriter.close();
    }

    /**
     * Checking if the calibration should be terminated (either complete or certain limit is reached)
     */
    private void checkIfComplete() {
        // Check if we reach the stage 2:
        if (!usePreviousOutput){
            usePreviousOutput = true;
            for (String mode : modes) {
                if (errorMap.get(mode).values().stream().mapToDouble(v -> v).sum() > 0.2) {
                    usePreviousOutput = false;
                    break;
                }
            }
        }

        // Check if the termination condition is reached
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

    /**
     * Calibrate the parameter in the config file and prepare for the next run
     */
    private void modifyConfig() {
        // get previous output directory
        String previousOutputDirectory = config.controler().getOutputDirectory();
        String runId = config.controler().getRunId();
        String previousOutputPlans = "/output_plans.xml.gz";
        if (runId != null && !runId.equals("")) {
            previousOutputPlans = "/" + runId + ".output_plans.xml.gz";
        }

        // Tune parameter
        parameterTuner.tune(config, errorMap, trips);

        // update output folder for next run
        int number = counter % (patience + 1); // Only keep the last (patience + 1) output. Override previous ones to prevent generating too many not very useful things...
        config.controler().setOutputDirectory(outputFolder + "/run-" + number);
        if (usePreviousOutput) {
            config.plans().setInputFile(previousOutputDirectory + previousOutputPlans);
            config.controler().setLastIteration(150);
            config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
        }
    }

    // Initializations
    private void readRelevantPersonsFile(String relevantPersonsFile) throws IOException {
        if (relevantPersonsFile != null && !relevantPersonsFile.equals("")) {
            // read persons live in the area
            try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(relevantPersonsFile)),
                    CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
                for (CSVRecord record : parser) {
                    relevantPersons.add(Id.createPersonId(record.get(0)));
                }
            }
        }
    }


    private void readReferenceData(String referenceDataFile) throws IOException {
        // For a sample reference data, please see the svn //TODO
        try (CSVParser parser = new CSVParser(Files.newBufferedReader(Path.of(referenceDataFile)),
                CSVFormat.DEFAULT.withDelimiter(',').withFirstRecordAsHeader())) {
            List<String> distancesGroups = new ArrayList<>();
            distancesGroups.add("mode"); // The first column is mode (this here is simply a placeholder)
            distancesGroups.addAll(Arrays.stream(distanceGrouping.getDistanceGroupings()).collect(Collectors.toList()));
            for (CSVRecord record : parser) {
                String mode = record.get(0);
                referenceMap.put(mode, new HashMap<>());
                for (int i = 1; i < record.size(); i++) {
                    referenceMap.get(mode).put(distancesGroups.get(i), Double.parseDouble(record.get(i)));
                }
            }
        }
    }


    private void calculateError(Map<String, Map<String, Double>> modeShare) {
        // Initialization
        for (String mode : modes) {
            errorMap.put(mode, new HashMap<>());
        }
        currentMaxAbsError = 0.0;
        currentSumAbsError = 0.0;

        // Calculate errors
        for (String mode : referenceMap.keySet()) {
            for (String distanceGroup : referenceMap.get(mode).keySet()) {
                double error = modeShare.get(mode).getOrDefault(distanceGroup, 0.0) - referenceMap.get(mode).get(distanceGroup);
                errorMap.get(mode).put(distanceGroup, error);
                currentSumAbsError += Math.abs(error);
                if (Math.abs(error) > currentMaxAbsError) {
                    currentMaxAbsError = Math.abs(error);
                }
            }
        }
        if (currentMaxAbsError >= maxAbsError && currentSumAbsError >= sumAbsError) {
            // No improvements for this run
            nonImprovingRuns++;
        } else {
            // Results have been improved
            nonImprovingRuns = 0;
            maxAbsError = Math.min(currentMaxAbsError, maxAbsError);
            sumAbsError = Math.min(currentSumAbsError, sumAbsError);
        }
    }
}
