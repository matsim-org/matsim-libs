package playground.boescpa.baseline;

import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a default config for the ivt baseline scenarios.
 *
 * @author boescpa
 */
public class ConfigCreator {

    final static String numberOfThreads = "8";
    final static String inbaseFiles = "";
    final static String writeOutInterval = "10";

    public static void main(String[] args) {
        int prctScenario = Integer.parseInt(args[1]); // the percentage of the scenario in percent (e.g. 1%-Scenario -> "1")
        new ConfigWriter(getIVTConfig(prctScenario)).write(args[0]);
    }

    private static Config getIVTConfig(final int prctScenario) {
        // Create config and add kti-scoring and destination choice
        Config config = ConfigUtils.createConfig(
                new KtiLikeScoringConfigGroup(),
                new DestinationChoiceConfigGroup());

        // Correct routing algorithm
        config.setParam("controler", "routingAlgorithmType", "FastAStarLandmarks");
        // Change write out intervals
        config.setParam("controler", "writeEventsInterval", writeOutInterval);
        config.setParam("controler", "writePlansInterval", writeOutInterval);
        config.setParam("controler", "writeSnapshotsInterval", writeOutInterval);
        config.setParam("counts", "writeCountsInterval", writeOutInterval);
        config.setParam("ptCounts", "ptCountsInterval", writeOutInterval);
        // Add f2l
        config.createModule(WorldConnectLocations.CONFIG_F2L);
        config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_INPUTF2LFile, "");
        // Set coordinate system
        config.setParam("global", "coordinateSystem", "CH1903_LV03_Plus");
        // Set location choice activities
        config.setParam("locationchoice", "flexible_types", "remote_home, remote_work, leisure, shop, escort_kids, escort_other");
        config.setParam("locationchoice", "epsilonScaleFactors", "0.7, 0.3, 0.1, 0.1, 0.1, 0.2");
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
        // Set threads to numberOfThreads
        config.setParam("global", "numberOfThreads", numberOfThreads);
        config.setParam("parallelEventHandling", "numberOfThreads", numberOfThreads);
        config.setParam("qsim", "numberOfThreads", numberOfThreads);
        // Account for prct-scenario
        config.setParam("counts", "countsScaleFactor", Double.toString(100d / prctScenario));
        config.setParam("ptCounts", "countsScaleFactor", Double.toString(100d/prctScenario));
        config.setParam("qsim", "flowCapacityFactor", Double.toString(prctScenario/100d));
        // Add files
        config.setParam("facilities", "inputFacilitiesFile", inbaseFiles + "facilities.xml.gz");
        config.setParam("f2l", "inputF2LFile", inbaseFiles + "facilitiesLinks.f2l");
        config.setParam("households", "inputFile", inbaseFiles + "households.xml.gz");
        config.setParam("network", "inputNetworkFile", inbaseFiles + "mmNetwork.xml.gz");
        config.setParam("plans", "inputPersonAttributesFile", inbaseFiles + "population_attributes.xml.gz");
        config.setParam("plans", "inputPlansFile", inbaseFiles + "population.xml.gz");
        config.setParam("transit", "transitScheduleFile", inbaseFiles + "mmSchedule.xml.gz");
        config.setParam("transit", "vehiclesFile", inbaseFiles + "mmVehicles.xml.gz");
        config.setParam("locationchoice", "prefsFile", inbaseFiles + "population_attributes.xml.gz");

        return config;
    }

    private static Map<String, Double> getStrategyDescr() {
        Map<String, Double> strategyDescr = new HashMap<>();
        strategyDescr.put("ChangeExpBeta", 0.5);
        strategyDescr.put("ReRoute", 0.2);
        strategyDescr.put("TimeAllocationMutator", 0.1);
        strategyDescr.put("org.matsim.contrib.locationchoice.BestReplyLocationChoicePlanStrategy", 0.1);
        strategyDescr.put("SubtourModeChoice", 0.1);
        return strategyDescr;
    }

    private static Map<String, Double> getActivityDescr() {
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
