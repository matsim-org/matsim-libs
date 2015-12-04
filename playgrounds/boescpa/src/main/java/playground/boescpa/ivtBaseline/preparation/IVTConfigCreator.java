package playground.boescpa.ivtBaseline.preparation;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a default config for the ivt baseline scenarios.
 *
 * @author boescpa
 */
public class IVTConfigCreator {

    protected final static String NUMBER_OF_THREADS = "8";
    protected final static String INBASE_FILES = "";
    protected final static String WRITE_OUT_INTERVAL = "10";
    protected final static String COORDINATE_SYSTEM = "CH1903_LV03_Plus";

    public static final String FACILITIES = "facilities.xml.gz";
    public static final String HOUSEHOLD_ATTRIBUTES = "household_attributes.xml.gz";
    public static final String HOUSEHOLDS = "households.xml.gz";
    public static final String POPULATION = "population.xml.gz";
    public static final String POPULATION_ATTRIBUTES = "population_attributes.xml.gz";
    public static final String NETWORK = "mmNetwork.xml.gz";
    public static final String SCHEDULE = "mmSchedule.xml.gz";
    public static final String VEHICLES = "mmVehicles.xml.gz";
    public static final String FACILITIES2LINKS = "facilitiesLinks.f2l";

    public static void main(String[] args) {
        int prctScenario = Integer.parseInt(args[1]); // the percentage of the scenario in percent (e.g. 1%-Scenario -> "1")
        // Create config and add kti-scoring and destination choice
        Config config = ConfigUtils.createConfig();
        new IVTConfigCreator().makeConfigIVT(config, prctScenario);
        new ConfigWriter(config).write(args[0]);
    }

    protected void makeConfigIVT(Config config, final int prctScenario) {
        // Correct routing algorithm
        config.setParam("controler", "routingAlgorithmType", "FastAStarLandmarks");
        // Change write out intervals
        config.setParam("controler", "writeEventsInterval", WRITE_OUT_INTERVAL);
        config.setParam("controler", "writePlansInterval", WRITE_OUT_INTERVAL);
        config.setParam("controler", "writeSnapshotsInterval", WRITE_OUT_INTERVAL);
        config.setParam("counts", "writeCountsInterval", WRITE_OUT_INTERVAL);
        config.setParam("ptCounts", "ptCountsInterval", WRITE_OUT_INTERVAL);
        // Add f2l
        config.createModule(WorldConnectLocations.CONFIG_F2L);
        config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_INPUTF2LFile, "");
        // Set coordinate system
        config.setParam("global", "coordinateSystem", COORDINATE_SYSTEM);
        // Add activity parameters
        //  <-> We have these as agent-specific parameters now...
        /*Map<String, Double> activityDescr = getActivityDescr();
        for (String activity : activityDescr.keySet()) {
            PlanCalcScoreConfigGroup.ActivityParams activitySet = new PlanCalcScoreConfigGroup.ActivityParams();
            activitySet.setActivityType(activity);
            activitySet.setTypicalDuration(activityDescr.get(activity));
            config.getModule(PlanCalcScoreConfigGroup.GROUP_NAME).addParameterSet(activitySet);
        }*/
        // Set coordinate system
        config.setParam("qsim", "endTime", "30:00:00");
        // Add strategies
        Map<String, Double> strategyDescr = getStrategyDescr();
        for (String strategy : strategyDescr.keySet()) {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName(strategy);
            strategySettings.setWeight(strategyDescr.get(strategy));
            config.getModule(StrategyConfigGroup.GROUP_NAME).addParameterSet(strategySettings);
        }
        // Activate transit
        config.setParam("transit", "useTransit", "true");
        // Set threads to NUMBER_OF_THREADS
        config.setParam("global", "numberOfThreads", NUMBER_OF_THREADS);
        config.setParam("parallelEventHandling", "numberOfThreads", NUMBER_OF_THREADS);
        config.setParam("qsim", "numberOfThreads", NUMBER_OF_THREADS);
        // Account for prct-scenario
        config.setParam("counts", "countsScaleFactor", Double.toString(100d / prctScenario));
        config.setParam("ptCounts", "countsScaleFactor", Double.toString(100d/prctScenario));
        config.setParam("qsim", "flowCapacityFactor", Double.toString(prctScenario/100d));
        // Add files
        config.setParam("facilities", "inputFacilitiesFile", INBASE_FILES + FACILITIES);
        config.setParam("f2l", "inputF2LFile", INBASE_FILES + FACILITIES2LINKS);
        config.setParam("households", "inputFile", INBASE_FILES + HOUSEHOLDS);
        config.setParam("households", "inputHouseholdAttributesFile", INBASE_FILES + HOUSEHOLD_ATTRIBUTES);
        config.setParam("network", "inputNetworkFile", INBASE_FILES + NETWORK);
        config.setParam("plans", "inputPersonAttributesFile", INBASE_FILES + POPULATION_ATTRIBUTES);
        config.setParam("plans", "inputPlansFile", INBASE_FILES + POPULATION);
        config.setParam("transit", "transitScheduleFile", INBASE_FILES + SCHEDULE);
        config.setParam("transit", "vehiclesFile", INBASE_FILES + VEHICLES);
    }

    protected Map<String, Double> getStrategyDescr() {
        Map<String, Double> strategyDescr = new HashMap<>();
        strategyDescr.put("ChangeExpBeta", 0.5);
        strategyDescr.put("ReRoute", 0.2);
        strategyDescr.put("TimeAllocationMutator", 0.1);
        strategyDescr.put("SubtourModeChoice", 0.1);
        return strategyDescr;
    }

    protected Map<String, Double> getActivityDescr() {
        Map<String, Double> activityDescr = new HashMap<>();
        activityDescr.put("home", 43200.);
        activityDescr.put("shop", 7200.);
        activityDescr.put("remote_home", 43200.);
        activityDescr.put("remote_work", 14400.);
        activityDescr.put("leisure", 14400.);
        activityDescr.put("escort_other", 3600.);
        activityDescr.put("education", 28800.);
        activityDescr.put("primary_work", 14400.);
        activityDescr.put("work", 14400.);
        activityDescr.put("escort_kids", 3600.);
        return activityDescr;
    }

}
