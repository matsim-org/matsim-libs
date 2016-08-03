package playground.kai.run;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.InflowConstraint;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

class KNBerlinControler {
	private static final Logger log = Logger.getLogger(KNBerlinControler.class);

	public static void main ( String[] args ) {
		OutputDirectoryLogging.catchLogEntries();

		log.info("Starting KNBerlinControler ...") ;
		
		final double sampleFactor = 0.02 ;

		// ### config:
		Config config = ConfigUtils.createConfig( new NoiseConfigGroup() ) ;
		
		// paths and related:
//		config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-base_ext.xml.gz");
//		config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-ba16_ext.xml.gz") ;
		config.network().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/network-ba16_17_ext.xml.gz") ;
		
		config.plans().setInputFile("~/kairuns/a100/baseplan_900s_routed.xml.gz") ;
		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );

		config.counts().setInputFile("~/shared-svn/studies/countries/de/berlin/counts/iv_counts/vmz_di-do.xml" ) ;
		config.counts().setOutputFormat("all");
		config.counts().setCountsScaleFactor(1./sampleFactor);
		config.counts().setWriteCountsInterval(100);
		
		config.controler().setOutputDirectory( System.getProperty("user.home") + "/kairuns/a100/output" );
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		// controler, global, and related:
		
		config.controler().setFirstIteration(0); // with something like "9" we don't get output events! 
		config.controler().setLastIteration(100); // with something like "9" we don't get output events! 
		config.controler().setWriteSnapshotsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(100);
		if ( config.controler().getLastIteration() < 10 ) {
			config.controler().setWritePlansUntilIteration(-1); // for fast turn-around, don't write at all
			config.controler().setWriteEventsUntilIteration(-1); 
		} else {
			config.controler().setWritePlansUntilIteration(-1); 
			config.controler().setWriteEventsUntilIteration(0); 
		}
		config.vspExperimental().setWritingOutputEvents(true);
		
		config.global().setCoordinateSystem("GK4");
		config.global().setRandomSeed(4711);
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);
		
		// activity parameters:

		createActivityParameters(config);
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}

		// threads:
		
		config.global().setNumberOfThreads(6);
		config.qsim().setNumberOfThreads(5);
		config.parallelEventHandling().setNumberOfThreads(1);
		
		// qsim:
		
		config.qsim().setEndTime(36*3600);

		config.controler().setMobsim( MobsimType.qsim.toString() );
		config.qsim().setFlowCapFactor( sampleFactor );
		//		config.qsim().setStorageCapFactor( Math.pow( sampleFactor, -0.25 ) ); // this version certainly is completely wrong.
		config.qsim().setStorageCapFactor(0.03);

		config.qsim().setTrafficDynamics( TrafficDynamics.withHoles );
		
		if ( config.qsim().getTrafficDynamics()==TrafficDynamics.withHoles ) {
			config.qsim().setInflowConstraint(InflowConstraint.maxflowFromFdiag);
		}

		config.qsim().setNumberOfThreads(6);
		config.qsim().setUsingFastCapacityUpdate(true);

		config.qsim().setUsingTravelTimeCheckInTeleportation(true) ;
		
//		config.qsim().setUsePersonIdForMissingVehicleId(false);
		// does not work

		//		config.controler().setMobsim(MobsimType.JDEQSim.toString());
		//		config.setParam(JDEQSimulation.NAME, JDEQSimulation.END_TIME, "36:00:00") ;
		//		config.setParam(JDEQSimulation.NAME, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
		//		config.setParam(JDEQSimulation.NAME, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
		//		config.setParam(JDEQSimulation.NAME, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;
		
		// mode parameters:
		
		createModeParameters(config);
		
		// strategy:

		{
			StrategySettings stratSets = new StrategySettings( ) ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() ) ;
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings( ) ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.name() ) ;
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		}
//		{
//			StrategySettings stratSets = new StrategySettings( ) ;
//			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode.name() );
//			stratSets.setWeight(0.1);
//			config.strategy().addStrategySettings(stratSets);
//			config.changeMode().setModes(new String[] {"walk","bike","car","pt","pt2"});
//		}
		{
//			StrategySettings stratSets = new StrategySettings( ) ;
//			stratSets.setStrategyName( DefaultStrategy.TimeAllocationMutator.name() );
//			stratSets.setWeight(0.1);
//			config.strategy().addStrategySettings(stratSets);
			config.timeAllocationMutator().setMutationRange(7200.);
			config.timeAllocationMutator().setAffectingDuration(false);
		}
		
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );

		ConfigUtils.loadConfig(config, System.getProperty("user.home") + "/kairuns/a100/additional-config.xml") ;

		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency();

		// ===

		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		for ( Link link : scenario.getNetwork().getLinks().values() ) {
			if ( link.getLength()<100 ) {
				link.setCapacity( 2. * link.getCapacity() ); // double capacity on short links, often roundabouts or short u-turns, etc., with the usual problem
			}
			if ( link.getFreespeed() < 77/3.6 ) {
				link.setFreespeed( 0.5 * link.getFreespeed() );
			}
		}
		
		// ===

		// prepare the control(l)er:
		Controler controler = new Controler( scenario ) ;

		controler.addControlerListener(new KaiAnalysisListener()) ;
		//		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		//		controler.setMobsimFactory(new OldMobsimFactory()) ;

		// run everything:
		controler.run();

		// ===
		// post-processing:

//		computeNoise(sampleFactor, config, scenario);

	}

	private static void computeNoise(final double sampleFactor, Config config, Scenario scenario) {
		// noise parameters
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModule("noise");
				
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);

		noiseParameters.setReceiverPointGap(2000.); // 200 is possible but overkill as long as this is not debugged.

		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);

		noiseParameters.setScaleFactor(1./sampleFactor); // yyyyyy sample size!!!!

		setTunnelLinkIds(noiseParameters);

		// ---

		String outputDirectory = config.controler().getOutputDirectory()+"/noise/" ;
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();		
		
		// ---

		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		mergeNoiseFiles(outputFilePath);
	}

	private static void createModeParameters(Config config) {
		// network modes:
		Collection<String> networkModes = Arrays.asList(new String[] {"car"}) ;
		config.plansCalcRoute().setNetworkModes(networkModes);
		{
			ModeParams params = new ModeParams("car") ;
			params.setConstant(-2.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}

		// teleported modes:
		
		{
			ModeRoutingParams pars = new ModeRoutingParams("walk") ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 3. / 3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);

			ModeParams params = new ModeParams("walk") ;
			params.setConstant(0);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("bike") ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 6. / 3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);

			ModeParams params = new ModeParams("bike") ;
			params.setConstant(-1);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("pt") ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 9. / 3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);

			ModeParams params = new ModeParams("pt") ;
			params.setConstant(-3.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("pt2") ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 18./3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);

			ModeParams params = new ModeParams("pt2") ;
			params.setConstant(-6.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("undefined") ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 50./3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);

			ModeParams params = new ModeParams("undefined") ;
			params.setConstant(-6.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
	}

	private static void createActivityParameters(Config config) {
		{
			ActivityParams params = new ActivityParams("home") ;
			params.setTypicalDuration(12*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("not specified") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
			// yyyy should probably be same as "home" in many situations: first activity of day
		}
		{
			ActivityParams params = new ActivityParams("leisure") ;
			params.setTypicalDuration(2*3600);
			params.setOpeningTime(10*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("shopping") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(8*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("work") ;
			params.setTypicalDuration(9*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(19*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("education") ;
			params.setTypicalDuration(8*3600);
			params.setOpeningTime(8*3600);
			params.setClosingTime(18*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("business") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(9*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("multiple") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("other") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("see a doctor") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("holiday / journey") ;
			params.setTypicalDuration(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("dummy") ; // Personenwirtschaftsverkehr von Sebastian Schneider
			params.setTypicalDuration(1*3600);
			config.planCalcScore().addActivityParams(params);
		}
	}

	private static void setTunnelLinkIds(NoiseConfigGroup noiseParameters) {
		// yyyyyy Same link ids?  Otherwise ask student
		Set<Id<Link>> tunnelLinkIDs = new HashSet<>();
		tunnelLinkIDs.add(Id.create("108041", Link.class));
		tunnelLinkIDs.add(Id.create("108142", Link.class));
		tunnelLinkIDs.add(Id.create("108970", Link.class));
		tunnelLinkIDs.add(Id.create("109085", Link.class));
		tunnelLinkIDs.add(Id.create("109757", Link.class));
		tunnelLinkIDs.add(Id.create("109919", Link.class));
		tunnelLinkIDs.add(Id.create("110060", Link.class));
		tunnelLinkIDs.add(Id.create("110226", Link.class));
		tunnelLinkIDs.add(Id.create("110164", Link.class));
		tunnelLinkIDs.add(Id.create("110399", Link.class));
		tunnelLinkIDs.add(Id.create("96503", Link.class));
		tunnelLinkIDs.add(Id.create("110389", Link.class));
		tunnelLinkIDs.add(Id.create("110116", Link.class));
		tunnelLinkIDs.add(Id.create("110355", Link.class));
		tunnelLinkIDs.add(Id.create("92604", Link.class));
		tunnelLinkIDs.add(Id.create("92603", Link.class));
		tunnelLinkIDs.add(Id.create("25651", Link.class));
		tunnelLinkIDs.add(Id.create("25654", Link.class));
		tunnelLinkIDs.add(Id.create("112540", Link.class));
		tunnelLinkIDs.add(Id.create("112556", Link.class));
		tunnelLinkIDs.add(Id.create("5052", Link.class));
		tunnelLinkIDs.add(Id.create("5053", Link.class));
		tunnelLinkIDs.add(Id.create("5380", Link.class));
		tunnelLinkIDs.add(Id.create("5381", Link.class));
		tunnelLinkIDs.add(Id.create("106309", Link.class));
		tunnelLinkIDs.add(Id.create("106308", Link.class));
		tunnelLinkIDs.add(Id.create("26103", Link.class));
		tunnelLinkIDs.add(Id.create("26102", Link.class));
		tunnelLinkIDs.add(Id.create("4376", Link.class));
		tunnelLinkIDs.add(Id.create("4377", Link.class));
		tunnelLinkIDs.add(Id.create("106353", Link.class));
		tunnelLinkIDs.add(Id.create("106352", Link.class));
		tunnelLinkIDs.add(Id.create("103793", Link.class));
		tunnelLinkIDs.add(Id.create("103792", Link.class));
		tunnelLinkIDs.add(Id.create("26106", Link.class));
		tunnelLinkIDs.add(Id.create("26107", Link.class));
		tunnelLinkIDs.add(Id.create("4580", Link.class));
		tunnelLinkIDs.add(Id.create("4581", Link.class));
		tunnelLinkIDs.add(Id.create("4988", Link.class));
		tunnelLinkIDs.add(Id.create("4989", Link.class));
		tunnelLinkIDs.add(Id.create("73496", Link.class));
		tunnelLinkIDs.add(Id.create("73497", Link.class));
		noiseParameters.setTunnelLinkIDsSet(tunnelLinkIDs);
	}

	static void mergeNoiseFiles(String outputFilePath) {
		final String receiverPointsFile = outputFilePath + "/receiverPoints/receiverPoints.csv" ;

		final String[] labels = { "immission", "consideredAgentUnits", "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/", outputFilePath + "/damages_receiverPoint/" };


		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setWorkingDirectory(workingDirectories);
		merger.setReceiverPointsFile(receiverPointsFile);
		merger.setLabel(labels);
		merger.setOutputFormat(OutputFormat.xyt);
		merger.setThreshold(1.);
		merger.setOutputDirectory(outputFilePath);
		merger.run();

	}

}