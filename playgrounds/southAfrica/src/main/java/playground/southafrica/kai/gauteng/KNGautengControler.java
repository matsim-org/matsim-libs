package playground.southafrica.kai.gauteng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.PlanStrategyRegistrar.Names;
import org.matsim.core.controler.PlanStrategyRegistrar.Selector;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingSchemeImpl;

import playground.southafrica.gauteng.routing.AutosensingTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.scoring.PersonSpecificUoMScoringFunctionFactory;
import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;
import playground.southafrica.utilities.Header;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;


/**
 * Design comments:<ul>
 * <li> The money (toll) is converted into utils both for the router and for the scoring.
 * <li> However, setting up the toll scheme in terms of disutilities does not seem right.
 * </ul>
 *
 */
public class KNGautengControler {
	static Logger log = Logger.getLogger(KNGautengControler.class) ;

	final static String MATSIM_SA_TRUNK = "/Users/nagel/ie-calvin/MATSim-SA/trunk/";
	final static String SANRAL2010 = MATSIM_SA_TRUNK + "data/sanral2010/" ;
	final static String outputDirectoryName = "/Users/nagel/kairuns/output3/";
	
//	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private Config config = null;

	private Double baseValueOfTime_h = 100.; 
	private Double valueOfTimeMultiplier = 4.;

	private void createConfiguration(String[] args) {
		Header.printHeader(KNGautengControler.class.toString(), args);

			config = ConfigUtils.loadConfig(SANRAL2010 + "config/kaiconfig.xml") ;

			// === sanral scenario start ===
//			config.network().setInputFile(SANRAL2010 + "network/gautengNetwork_CleanV2.xml.gz");
//			config.plans().setInputFile(SANRAL2010 + "plans/car-com-bus-taxi-ext_plans_2009_1pct-with-routesV0.xml.gz") ;
//			config.roadpricing().setTollLinksFile(SANRAL2010 + "toll/gauteng_toll_joint_weekday_02.xml" );
//			double sampleFactor = 0.01 ;
//			config.counts().setCountsFileName("/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/counts/2007/Counts_Wednesday_Total.xml.gz");
//			config.counts().setCountsScaleFactor(100);
//			config.counts().setOutputFormat("kml,txt");
			// === sanral scenario end ===
			
			// === equil test scenario start ===
			config.network().setInputFile("../../../matsim/trunk/examples/equil/network.xml");
			config.plans().setInputFile("../../../matsim/trunk/examples/equil/plans2000.xml.gz" ) ;
			config.roadpricing().setTollLinksFile("../../../matsim/trunk/examples/equil/toll.xml" ) ;
			double sampleFactor = 1. ;
			config.counts().setCountsFileName(null) ;
			{
				ActivityParams params = new ActivityParams("w") ;
				params.setTypicalDuration(2.*3600.);
//				params.setOpeningTime(8.*3600.);
//				params.setLatestStartTime(8.*3600.);
				config.planCalcScore().addActivityParams(params);
			}
			{
				ActivityParams params = new ActivityParams("h") ;
				params.setTypicalDuration(12.*3600.);
				config.planCalcScore().addActivityParams(params);
			}
			config.planCalcScore().setBrainExpBeta(10.);
			// === equil test scenario end ===
			
			config.controler().setOutputDirectory(outputDirectoryName);
			config.controler().setLastIteration(1000);
			config.controler().setWriteEventsInterval(10);
			config.controler().setWriteSnapshotsInterval(0);
			config.controler().setWritePlansInterval(100);
			
//			config.controler().setMobsim(MobsimType.JDEQSim.toString());
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.CAR_SIZE, "7.5" ) ;
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.GAP_TRAVEL_SPEED, "15.0" ) ; 
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.MINIMUM_INFLOW_CAPACITY, "1800." ) ;
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
//			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;

			config.controler().setMobsim(MobsimType.qsim.toString() );
			config.qsim().setEndTime(36.*3600.);
			config.qsim().setFlowCapFactor(sampleFactor);
			config.qsim().setStorageCapFactor( Math.pow(sampleFactor,-0.25) );
			
			config.global().setNumberOfThreads(1);
			
			// strategy:
			{
				StrategySettings stratSets = new StrategySettings(new IdImpl(1)) ;
				stratSets.setModuleName(Selector.ChangeExpBeta.toString());
				stratSets.setProbability(0.8);
				config.strategy().addStrategySettings(stratSets);
			}
			{
				StrategySettings stratSets = new StrategySettings(new IdImpl(2)) ;
				stratSets.setModuleName(Names.ReRoute.toString()) ;
				stratSets.setProbability(0.1);
				config.strategy().addStrategySettings(stratSets);
			}
//			{
//				StrategySettings stratSets = new StrategySettings(new IdImpl(3)) ;
//				stratSets.setModuleName(Names.TimeAllocationMutator.toString()) ;
//				stratSets.setProbability(0.1);
//				config.strategy().addStrategySettings(stratSets);
//			}
			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
//			config.strategy().setPlanSelectorForRemoval("abd");
			
			final int firstIt = config.controler().getFirstIteration();
			long diff = config.controler().getLastIteration() - firstIt ;
			config.vspExperimental().addParam( VspExperimentalConfigKey.scoreMSAStartsAtIteration,  Long.toString((long)(firstIt + 0.8*diff)) ) ;
			
			config.timeAllocationMutator().setMutationRange(7200.);
			
			// VSP DEFAULTS:
			config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
			config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
			
			config.vspExperimental().addParam( VspExperimentalConfigKey.vspDefaultsCheckingLevel, VspExperimentalConfigGroup.WARN ) ;
		
	}

	private void run () {
		if (config.scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		// === SCENARIO: ===
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// CONSTRUCT UTILITY OF MONEY:
//		UtilityOfMoneyI personSpecificUtilityOfMoney = 
//				new GautengUtilityOfMoney( config.planCalcScore() , baseValueOfTime_h, valueOfTimeMultiplier) ;
		UtilityOfMoneyI personSpecificUtilityOfMoney = 
				new UtilityOfMoneyI(){
			@Override
			public double getMarginalUtilityOfMoney(Id personId) {
				return 100./(1.+Double.parseDouble( personId.toString() ) ) ;
			}
		} ;
		final ScoringFunctionFactory scoringFunctionFactory = 
				new PersonSpecificUoMScoringFunctionFactory(scenario.getConfig(), scenario.getNetwork(), personSpecificUtilityOfMoney );

		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingSchemeImpl scheme = 
//				new GautengRoadPricingScheme( config.roadpricing().getTollLinksFile(), scenario.getNetwork() , scenario.getPopulation() );
				new RoadPricingSchemeImpl() ;
		new RoadPricingReaderXMLv1(scheme).parse( config.roadpricing().getTollLinksFile() );

		final TravelDisutilityFactory travelDisutilityFactory = 
//				new PersonSpecificTravelDisutilityInclTollFactory( scheme, personSpecificUtilityOfMoney );
				new AutosensingTravelDisutilityInclTollFactory(scheme, scenario, scoringFunctionFactory);
		
		// === CONTROLER: ===

		final Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener( new GenerationOfMoneyEvents( scenario.getNetwork(), scenario.getPopulation(), scheme) ) ;
		controler.setScoringFunctionFactory( scoringFunctionFactory );

		// insert into routing:
		controler.setTravelDisutilityFactory( travelDisutilityFactory );
		
		// plans removal:
		controler.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getControler().getStrategyManager().setPlanSelectorForRemoval(new MyPlanSelectorForRemoval(scenario.getNetwork()));
			}
		});
		
		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener()) ;

		// RUN:
		controler.run();

	}

	
	public static void main( String[] args ) {
		KNGautengControler controler = new KNGautengControler() ;
		controler.createConfiguration(args); ;
		controler.run();
	}
	
	@Test
	public final void testOne() {
		this.createConfiguration(null);
		this.config.controler().setLastIteration(1);
		this.run(); 
		
		final String baseDir = "/Users/nagel/gauteng-kairuns/base/" ;
		final String filePath = "ITERS/it.1/1.tripdurations.txt" ;
		
		final String originalFileName = baseDir + filePath ;
		long originalCheckSum = CRCChecksum.getCRCFromFile(originalFileName);
		final String revisedFileName = outputDirectoryName + filePath ;
		long revisedCheckSum = CRCChecksum.getCRCFromFile(revisedFileName);

		if ( revisedCheckSum != originalCheckSum ) {
			System.out.flush() ;
			log.warn("checksums are not the same; making explicit comparison. When the difference is large, this may take forever ...") ;
			System.err.flush() ;

			List<String> original = fileToLines(originalFileName);
			List<String> revised  = fileToLines(revisedFileName);

			Patch patch = DiffUtils.diff(original, revised);

			for (Delta delta: patch.getDeltas()) {
				System.out.flush() ;
				System.err.println(delta.getOriginal());
				System.err.println(delta.getRevised());
				System.err.flush() ;
			}

		}
		Assert.assertEquals("different files", originalCheckSum, revisedCheckSum);
		log.info("files are the same");

		
	}
	
	// Helper method for get the file content
	private static List<String> fileToLines(String filename) {
		List<String> lines = new LinkedList<String>();
		String line = "";
		try {
			BufferedReader in = IOUtils.getBufferedReader(filename);
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}


}
