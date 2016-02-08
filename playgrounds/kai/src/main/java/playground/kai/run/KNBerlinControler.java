package playground.kai.run;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.noise2.NoiseParameters;
import playground.ikaddoura.noise2.NoiseWriter;
import playground.ikaddoura.noise2.data.GridParameters;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.handler.NoiseTimeTracker;
import playground.ikaddoura.noise2.handler.PersonActivityTracker;
import playground.ikaddoura.noise2.utils.MergeNoiseCSVFile;
import playground.ikaddoura.noise2.utils.MergeNoiseCSVFile.OutputFormat;

class KNBerlinControler {
	private static final Logger log = Logger.getLogger("blabla");

	public static void main ( String[] args ) {
		log.warn("here") ;

		// ### prepare the config:
		Config config = ConfigUtils.loadConfig( "/Users/nagel/kairuns/a100/config.xml" ) ;

		// paths:
		//		config.network().setInputFile("/Users/nagel/");
		config.controler().setOutputDirectory("/Users/nagel/kairuns/a100/output/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.controler().setFirstIteration(100); // with something like "9" we don't get output events! 
		config.controler().setLastIteration(100); // with something like "9" we don't get output events! 
		config.controler().setWriteSnapshotsInterval(100);
		config.controler().setWritePlansInterval(200);
		config.controler().setWriteEventsInterval(100);
		config.vspExperimental().setWritingOutputEvents(true);

		config.global().setNumberOfThreads(6);
		config.qsim().setNumberOfThreads(5);
		config.parallelEventHandling().setNumberOfThreads(1);

		final double sampleFactor = 0.02 ;
		config.controler().setMobsim( MobsimType.qsim.toString() );
		config.qsim().setFlowCapFactor( sampleFactor );
		//		config.qsim().setStorageCapFactor( Math.pow( sampleFactor, -0.25 ) ); // this version certainly is completely wrong.
		config.qsim().setStorageCapFactor(0.03);
		config.qsim().setTrafficDynamics( TrafficDynamics.withHoles );
		config.qsim().setUsingFastCapacityUpdate(false);
		config.qsim().setNumberOfThreads(6);
		config.qsim().setUsingFastCapacityUpdate(true);

		//		config.controler().setMobsim(MobsimType.JDEQSim.toString());
		//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
		//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
		//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
		//		config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;

		config.timeAllocationMutator().setMutationRange(7200.);
		config.timeAllocationMutator().setAffectingDuration(false);

		config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.8);
		for ( ActivityParams params : config.planCalcScore().getActivityParams() ) {
			params.setTypicalDurationScoreComputation( TypicalDurationScoreComputation.relative );
		}

		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		config.plans().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration );

		{
			ModeRoutingParams pars = config.plansCalcRoute().getOrCreateModeRoutingParams("pt") ;
			pars.setBeelineDistanceFactor(1.5);
			pars.setTeleportedModeSpeed( 20. / 3.6 );
		}
		{
			ModeRoutingParams pars = new ModeRoutingParams("pt2") ;
			pars.setBeelineDistanceFactor(1.5);
			pars.setTeleportedModeSpeed( 40. / 3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);
		}
		{
			ModeParams params = config.planCalcScore().getOrCreateModeParams("pt") ;
			params.setConstant(-2.);
			params.setMarginalUtilityOfTraveling(0.);
		}
		{
			ModeParams params = new ModeParams("pt2") ;
			params.setConstant(-4.);
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}

		{
			StrategySettings stratSets = new StrategySettings( ConfigUtils.createAvailableStrategyId(config) ) ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode.toString() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
		}
		config.setParam( ChangeLegModeConfigGroup.CONFIG_MODULE, ChangeLegModeConfigGroup.CONFIG_PARAM_MODES, "walk,bike,car,pt,pt2" );

		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort );
		config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl());
		config.checkConsistency();

		// ===

		// prepare the scenario
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;

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

		// grid parameters
		GridParameters gridParameters = new GridParameters();

		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);

		gridParameters.setReceiverPointGap(200.);

		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForSpatialFunctionality(consideredActivitiesForDamages);

		// noise parameters
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setScaleFactor(1./sampleFactor); // yyyyyy sample size!!!!


		// yyyyyy Same link ids?  Otherwise ask student
		Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
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
		noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);

		// ---

		String outputDirectory = config.controler().getOutputDirectory() ;
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();

		EventsManager events = EventsUtils.createEventsManager();

		EventWriterXML eventWriter = new EventWriterXML(outputFilePath + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
		events.addHandler(eventWriter);

		NoiseContext noiseContext = new NoiseContext(scenario, gridParameters, noiseParameters);
		noiseContext.initialize();
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/", true);

		NoiseTimeTracker timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
		timeTracker.setUseCompression(true);
		events.addHandler(timeTracker);

		PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
		events.addHandler(actTracker);

		log.info("Reading events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		log.info("Reading events file... Done.");

		timeTracker.computeFinalTimeIntervals();

		eventWriter.closeFile();
		log.info("Noise calculation completed.");

		// ---

		mergeNoiseFiles(outputFilePath);



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