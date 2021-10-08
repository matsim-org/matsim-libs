package org.matsim.application.automatedCalibration;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    private final String configFile;

    private static final Logger log = LogManager.getLogger(AutomaticScenarioCalibrator.class);
    private final Map<String, Map<Double, Double>> errorMap = new HashMap<>();
    private final Map<String, Map<Double, Double>> referenceMap = new HashMap<>();
    private int counter = 0;
    private int nonImprovingRuns = 0;
    private double maxAbsError = 1.0;
    private boolean complete = false;
    private long startTime;
    private final Config config;
    private final ParameterTuner parameterTuner;

    public AutomaticScenarioCalibrator(String configFile, String referenceDataFile, double targetError, long maxRunningTime, int patience) throws IOException {
        this.targetError = targetError;
        this.maxRunningTime = maxRunningTime;
        this.patience = patience;
        this.configFile = configFile;
        this.config = ConfigUtils.loadConfig(configFile);
        this.parameterTuner = new SimpleParameterTuner(targetError);
        readReferenceData(referenceDataFile);
    }

    private void calibrate() {
        startTime = System.currentTimeMillis();
        config.controler().setOutputDirectory("./auto-calibration/run-0");
        // Auto tuning loop
        while (true) {
            runSimulation();
            analyzeResults();
            checkIfComplete();
            if (complete) {
                break;
            }
            counter++;
            modifyConfig();
        }
    }

    public abstract void runSimulation();

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

    private void modifyConfig() {
        // update input and output
        config.controler().setOutputDirectory("./auto-calibration/run-" + counter);
        // Tune parameter
        parameterTuner.tune(config, errorMap);
        // overwrite the old config file (the design here may be improved in the future)
        ConfigUtils.writeConfig(config, configFile);
    }

    private void analyzeResults() {
        // Get output plan
        Population outputPlans = PopulationUtils.readPopulation(config.controler().getOutputDirectory() + "/outputplans.xml.gz"); //TODO
        // Analyze mode share of the output plan
        Map<String, Map<Double, Double>> modeShare = analyzeModeShare(outputPlans);
        // update error map
        updateErrorMap(modeShare);
    }

    protected Map<String, Map<Double, Double>> analyzeModeShare(Population outputPlans) {
        // Currently, only support 1 distance categorization: 0-1, 1-2, 2-5, 5-10, 10-20, 20+
        // TODO include person filter
        Map<String, Map<Double, Double>> modeCount = new HashMap<>();
        Map<String, Map<Double, Double>> modeShare = new HashMap<>();
        int totalTrips = 0;
        MainModeIdentifier mainModeIdentifier = new DefaultAnalysisMainModeIdentifier();
        for (Person person : outputPlans.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
            for (TripStructureUtils.Trip trip : trips) {
                String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
                double euclideanDistance = CoordUtils.calcEuclideanDistance
                        (trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
                // TODO mode count
                totalTrips++;
            }


        }

        for (String mode: modeCount.keySet()) {
            for (Double distance: modeCount.get(mode).keySet()) {
                double share = modeCount.get(mode).get(distance) / totalTrips;
                modeShare.get(mode).put(distance, share);
            }
        }
        return modeShare;
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

    private void updateErrorMap(Map<String, Map<Double, Double>> modeShare) {
        double maxAbsErrorForThisRun = 0.0;
        for (String mode : referenceMap.keySet()) {
            for (double distance : referenceMap.get(mode).keySet()) {
                double error = modeShare.get(mode).get(distance) - referenceMap.get(mode).get(distance);
                errorMap.get(mode).put(distance, error);
                if (Math.abs(error) > maxAbsErrorForThisRun) {
                    maxAbsErrorForThisRun = Math.abs(error);
                }
            }
        }
        if (maxAbsErrorForThisRun >= maxAbsError) {
            // No improvements for this run
            nonImprovingRuns++;
        } else {
            // Results have been improved
            nonImprovingRuns = 0;
            maxAbsError = maxAbsErrorForThisRun;
        }
    }
}
