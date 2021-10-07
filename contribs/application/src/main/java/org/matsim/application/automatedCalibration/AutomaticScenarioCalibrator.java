package org.matsim.application.automatedCalibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;

import java.util.HashMap;
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
    private final double maxAllowedError;
    private final String configFile;

    private static final Logger log = LogManager.getLogger(AutomaticScenarioCalibrator.class);
    private final Map<String, Map<String, Double>> errorMap = new HashMap<>();
    private int counter = 0;
    private int nonImprovingRuns = 0;
    private double maxError;
    private boolean complete = false;
    private long startTime;
    private final Config config;

    public AutomaticScenarioCalibrator(String configFile, double maxAllowedError, long maxRunningTime, int patience) {
        this.maxAllowedError = maxAllowedError;
        this.maxRunningTime = maxRunningTime;
        this.patience = patience;
        this.configFile = configFile;
        this.config = ConfigUtils.loadConfig(configFile);
    }


    private void calibrate() {
        startTime = System.currentTimeMillis();
        // Step 1 set parameter and initialization
        config.controler().setOutputDirectory("./auto-calibration/run_0");

        // Step 2 run loop
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

    private void modifyConfig() {
        // update input and output
        config.controler().setOutputDirectory("./auto-calibration/run-" + counter);

        // update parameters
        config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(100);
        double originalDistanceCost = config.planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfDistance();

        // overwrite the old config file (the design here may be improved in the future)
        ConfigUtils.writeConfig(config, configFile);
    }

    public abstract void runSimulation();

    private void analyzeResults() {
        // Get output plan
        Population outputPlans = PopulationUtils.readPopulation(config.controler().getOutputDirectory() + "/outputplans.xml.gz"); //TODO

        // Analyze mode share of the output plan

        // Compare to the reference

        // Compute the changes

    }

    private void checkIfComplete() {
        // Case 1: Everything is within range
        if (maxError < maxAllowedError) {
            complete = true;
            log.info("Auto-tuning process ends successfully with all error within range!");
        }

        // Case 2: No improvements after certain number of runs
        if (nonImprovingRuns > patience) {
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
}
