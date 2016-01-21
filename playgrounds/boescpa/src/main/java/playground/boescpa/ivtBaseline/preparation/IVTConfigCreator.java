package playground.boescpa.ivtBaseline.preparation;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.socnetsim.framework.replanning.modules.BlackListedTimeAllocationMutator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import playground.ivt.replanning.BlackListedTimeAllocationMutatorConfigGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Creates a default config for the ivt baseline scenarios.
 *
 * @author boescpa
 */
public class IVTConfigCreator {

	// Scenario
    protected final static int NUMBER_OF_THREADS = 8;
    protected final static String INBASE_FILES = "";
    protected final static int WRITE_OUT_INTERVAL = 10;
    protected final static String COORDINATE_SYSTEM = "CH1903_LV03_Plus";

	// ActivityTypes
	public static final String HOME = "home";
	public static final String REMOTE_HOME = "remote_home";
	public static final String WORK = "work";
	public static final String REMOTE_WORK = "remote_work";
	public static final String EDUCATION = "education";
	public static final String LEISURE = "leisure";
	public static final String SHOP = "shop";
	public static final String ESCORT_KIDS = "escort_kids";
	public static final String ESCORT_OTHER = "escort_other";

	// Files
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
		config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);
        // Change write out intervals
		config.controler().setWriteEventsInterval(WRITE_OUT_INTERVAL);
		config.controler().setWritePlansInterval(WRITE_OUT_INTERVAL);
		config.controler().setWriteSnapshotsInterval(WRITE_OUT_INTERVAL);
		config.counts().setWriteCountsInterval(WRITE_OUT_INTERVAL);
		config.ptCounts().setPtCountsInterval(WRITE_OUT_INTERVAL);
        // Add f2l
        config.createModule(WorldConnectLocations.CONFIG_F2L);
        // Set coordinate system
		config.global().setCoordinateSystem(COORDINATE_SYSTEM);
        // Add activity parameters
        //  <-> We have these as agent-specific parameters now...
        /*Map<String, Double> activityDescr = getActivityDescr();
        for (String activity : activityDescr.keySet()) {
            PlanCalcScoreConfigGroup.ActivityParams activitySet = new PlanCalcScoreConfigGroup.ActivityParams();
            activitySet.setActivityType(activity);
            activitySet.setTypicalDuration(activityDescr.get(activity));
            config.planCalcScore().addParameterSet(activitySet);
        }*/
        // Set end time
		config.qsim().setEndTime(108000); // 30:00:00
        // Add strategies
        Map<String, Double> strategyDescr = getStrategyDescr();
        for (String strategy : strategyDescr.keySet()) {
            StrategyConfigGroup.StrategySettings strategySettings = new StrategyConfigGroup.StrategySettings();
            strategySettings.setStrategyName(strategy);
            strategySettings.setWeight(strategyDescr.get(strategy));
            config.getModule(StrategyConfigGroup.GROUP_NAME).addParameterSet(strategySettings);
        }
		// Add black listed time mutation and the black listed modes:
		// (black listed are all modes which are not free for the agent to decide the start and end times)
		BlackListedTimeAllocationMutatorConfigGroup blTAM = new BlackListedTimeAllocationMutatorConfigGroup();
		Set<String> timeMutationBlackList = new HashSet<>();
		timeMutationBlackList.add(WORK);
		timeMutationBlackList.add(REMOTE_WORK);
		timeMutationBlackList.add(EDUCATION);
		timeMutationBlackList.add(ESCORT_KIDS);
		timeMutationBlackList.add(ESCORT_OTHER);
		blTAM.setBlackList(timeMutationBlackList);
		config.addModule(blTAM);
        // Activate transit and correct it to ivt-experience
		config.transit().setUseTransit(true);
		config.planCalcScore().setUtilityOfLineSwitch(-2.0);
		config.transitRouter().setSearchRadius(2000.0);
		PlanCalcScoreConfigGroup.ModeParams transitWalkSet = getModeParamsTransitWalk(config);
		transitWalkSet.setMarginalUtilityOfTraveling(-12.0);
        // Set threads to NUMBER_OF_THREADS
		config.global().setNumberOfThreads(NUMBER_OF_THREADS);
		config.parallelEventHandling().setNumberOfThreads(NUMBER_OF_THREADS);
		config.qsim().setNumberOfThreads(NUMBER_OF_THREADS);
        // Account for prct-scenario
		config.counts().setCountsScaleFactor(100d / prctScenario);
		config.ptCounts().setCountsScaleFactor(100d / prctScenario);
		config.qsim().setFlowCapFactor(prctScenario / 100d);
        // Add files
		config.facilities().setInputFile(INBASE_FILES + FACILITIES);
		config.setParam(WorldConnectLocations.CONFIG_F2L, WorldConnectLocations.CONFIG_F2L_INPUTF2LFile, INBASE_FILES + FACILITIES2LINKS);
		config.households().setInputFile(INBASE_FILES + HOUSEHOLDS);
		config.households().setInputHouseholdAttributesFile(INBASE_FILES + HOUSEHOLD_ATTRIBUTES);
		config.network().setInputFile(INBASE_FILES + NETWORK);
		config.plans().setInputFile(INBASE_FILES + POPULATION_ATTRIBUTES);
		config.plans().setInputPersonAttributeFile(INBASE_FILES + POPULATION);
		config.transit().setTransitScheduleFile(INBASE_FILES + SCHEDULE);
		config.transit().setVehiclesFile(INBASE_FILES + VEHICLES);
    }

	private PlanCalcScoreConfigGroup.ModeParams getModeParamsTransitWalk(Config config) {
		PlanCalcScoreConfigGroup.ModeParams transitWalkSet = config.planCalcScore().getOrCreateModeParams(TransportMode.transit_walk);
		transitWalkSet.setConstant(config.planCalcScore().getOrCreateModeParams(TransportMode.walk).getConstant());
		transitWalkSet.setMarginalUtilityOfDistance(config.planCalcScore().getOrCreateModeParams(TransportMode.walk).getMarginalUtilityOfDistance());
		transitWalkSet.setMarginalUtilityOfTraveling(config.planCalcScore().getOrCreateModeParams(TransportMode.walk).getMarginalUtilityOfTraveling());
		transitWalkSet.setMonetaryDistanceRate(config.planCalcScore().getOrCreateModeParams(TransportMode.walk).getMonetaryDistanceRate());
		return transitWalkSet;
	}

	protected Map<String, Double> getStrategyDescr() {
        Map<String, Double> strategyDescr = new HashMap<>();
        strategyDescr.put("ChangeExpBeta", 0.5);
        strategyDescr.put("ReRoute", 0.2);
		strategyDescr.put("BlackListedTimeAllocationMutator", 0.1);
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
