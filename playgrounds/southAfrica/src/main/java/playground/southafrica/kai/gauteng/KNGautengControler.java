package playground.southafrica.kai.gauteng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.controler.PlanStrategyRegistrar.Names;
import org.matsim.core.controler.PlanStrategyRegistrar.Selector;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.roadpricing.RoadPricingReaderXMLv1;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollFactorOLD;
import playground.southafrica.gauteng.roadpricingscheme.SanralTollVehicleType;
import playground.southafrica.gauteng.roadpricingscheme.TollFactorI;
import playground.southafrica.gauteng.scoring.GautengScoringFunctionFactory;
import playground.southafrica.gauteng.scoring.GenerationOfMoneyEvents;
import playground.southafrica.gauteng.utilityofmoney.GautengUtilityOfMoney;
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
	final static String outputDirectoryName = "/Users/nagel/kairuns/output2/";
	
	static enum Case {equil,gauteng} ;
	static final Case ccc = Case.equil ;
	
	private Config config = null;

	private Double baseValueOfTime_h = 110.; 
	private Double valueOfTimeMultiplier = 4.;

	private void createConfiguration(String[] args) {
		Header.printHeader(KNGautengControler.class.toString(), args);

			config = ConfigUtils.loadConfig(SANRAL2010 + "config/kaiconfig.xml") ;

			config.controler().setOutputDirectory(outputDirectoryName);
			config.controler().setLastIteration(100);
			config.controler().setWriteEventsInterval(10);
			config.controler().setWriteSnapshotsInterval(0);
			config.controler().setWritePlansInterval(100);
			
			config.global().setRandomSeed(4713);
			
			double sampleFactor = 1. ;
			switch( ccc ) {
			case equil:
				// === equil test scenario start ===
				config.network().setInputFile("../../../matsim/trunk/examples/equil/network.xml");
				config.plans().setInputFile("../../../matsim/trunk/examples/equil/plans2000.xml.gz" ) ;
				config.roadpricing().setTollLinksFile("../../../matsim/trunk/examples/equil/toll.xml" ) ;
				config.counts().setCountsFileName(null) ;
				{
					ActivityParams params = new ActivityParams("w") ;
					params.setTypicalDuration(2.*3600.);
//					params.setOpeningTime(8.*3600.);
//					params.setLatestStartTime(8.*3600.);
					config.planCalcScore().addActivityParams(params);
				}
				{
					ActivityParams params = new ActivityParams("h") ;
					params.setTypicalDuration(12.*3600.);
					config.planCalcScore().addActivityParams(params);
				}
				config.planCalcScore().setBrainExpBeta(1.);
				// === equil test scenario end ===
				break;
			case gauteng:
				// === sanral scenario start ===
				config.network().setInputFile(SANRAL2010 + "network/gautengNetwork_CleanV2.xml.gz");
//				config.plans().setInputFile(SANRAL2010 + "plans/car-com-bus-taxi-ext_plans_2009_1pct-with-routesV0.xml.gz") ;
				config.plans().setInputFile("/Users/nagel/shared-svn/projects/freight/studies/gauteng-kairuns/runs/2013-11-30-14h50/output_plans.xml.gz") ;

				
				config.roadpricing().setTollLinksFile(SANRAL2010 + "toll/gauteng_toll_joint_weekday_02.xml" );
				sampleFactor = 0.01 ;
				config.counts().setCountsFileName("/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/counts/2007/Counts_Wednesday_Total.xml.gz");
				config.counts().setCountsScaleFactor(100);
				config.counts().setOutputFormat("kml,txt");
				// === sanral scenario end ===
				break;
			default:
				throw new RuntimeException("todo") ;
			}
			
			
			config.controler().setMobsim(MobsimType.JDEQSim.toString());
			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00") ;
			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, Double.toString(sampleFactor) ) ;
			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;
			config.setParam(JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, Double.toString( Math.pow(sampleFactor, -0.25)) ) ;

//			config.controler().setMobsim(MobsimType.qsim.toString() );
//			config.qsim().setEndTime(36.*3600.);
//			config.qsim().setFlowCapFactor(sampleFactor);
//			config.qsim().setStorageCapFactor( Math.pow(sampleFactor,-0.25) );
			
			config.global().setNumberOfThreads(1); // replanning!
			// 4 instead of 1 reduces replanning times from about 100sec to about 70sec
			// I have not taken measurements with "2" but the computer is difficult to use with it.
			
//			config.parallelEventHandling().setNumberOfThreads(1); 
			// using this (with 1) reduces jdeqsim times from about 29sec to about 19sec
			// This does not seem as bad as the replanning.  Let's leave it for the time being.
			// I think it is also bad, it just does not last that long.
			
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

			config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
			{			
				final int firstIt = config.controler().getFirstIteration();
				long diff = config.controler().getLastIteration() - firstIt ;
				config.vspExperimental().setScoreMSAStartsAtIteration( (int)(firstIt + 0.8*diff) ) ;
			}
			
			// VSP DEFAULTS:
			config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
			config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration.toString());
			config.timeAllocationMutator().setMutationRange(7200.);
			
			config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.ABORT ) ;
	}

	private void run () {
		if (config.scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}

		// === SCENARIO: ===
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		config.scenario().setUseVehicles(true);
		
		TollFactorI tollFactor = new SanralTollFactorOLD() ;
		
		Map<SanralTollVehicleType,Double> cnt = new HashMap<SanralTollVehicleType,Double>() ;
		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			SanralTollVehicleType type = tollFactor.typeOf( person.getId() ) ;
			if ( cnt.get(type)==null ) {
				cnt.put(type, 0.) ;
			}
			cnt.put( type, 1. + cnt.get(type) ) ;
		}
		for ( SanralTollVehicleType type : SanralTollVehicleType.values() ) {
			log.info( String.format( "type: %30s; cnt: %8.0f", type.toString() , cnt.get(type) ) );
		}
		
		ObjectAttributes pAttribs = scenario.getPopulation().getPersonAttributes() ;
		String ATTR_NAME = config.plans().getSubpopulationAttributeName() ;
		
		VehicleType truckVehType = new VehicleTypeImpl(new IdImpl("truck") ) ;
		truckVehType.setLength(20.); 
		truckVehType.setMaximumVelocity( 100./3.6);
		
		Vehicles vehicles = ((ScenarioImpl) scenario).getVehicles() ;

		for ( Person person : scenario.getPopulation().getPersons().values() ) {
			String attrib = (String) pAttribs.getAttribute(person.getId().toString(), ATTR_NAME ) ;
//			if ( attrib.equals("commercial") ) {
				Id vehicleId = person.getId() ;
				final Vehicle truck = VehicleUtils.getFactory().createVehicle(vehicleId, truckVehType );
				vehicles.addVehicle(truck);
//			}
		}
		
		// CONSTRUCT UTILITY OF MONEY:
		UtilityOfMoneyI personSpecificUtilityOfMoney ;
		switch (ccc ) {
		case equil:
			personSpecificUtilityOfMoney = new UtilityOfMoneyI(){
				@Override
				public double getMarginalUtilityOfMoney(Id personId) {
					return 100./(1.+Double.parseDouble( personId.toString() ) ) ;
				}
			} ;
			break;
		case gauteng:
			personSpecificUtilityOfMoney = new GautengUtilityOfMoney( scenario , config.planCalcScore(), baseValueOfTime_h, valueOfTimeMultiplier, tollFactor) ;
			break;
		default:
			throw new RuntimeException("missing") ;
		}
		
		// SCORING FUNCTION:
		final ScoringFunctionFactory scoringFunctionFactory = 
				new GautengScoringFunctionFactory(scenario, personSpecificUtilityOfMoney );

		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingScheme scheme = null ;
		switch( ccc ) {
		case equil:
			RoadPricingSchemeImpl schemeImpl = new RoadPricingSchemeImpl() ;
			new RoadPricingReaderXMLv1(schemeImpl).parse( config.roadpricing().getTollLinksFile() );
			scheme = schemeImpl ;
			break;
		case gauteng:
			scheme = new GautengRoadPricingScheme( config.roadpricing().getTollLinksFile(), scenario.getNetwork() , scenario.getPopulation(), tollFactor );
			break;
		default:
			break;
		}
		
		// === CONTROLER: ===

		final Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener( new GenerationOfMoneyEvents( scenario.getNetwork(), scenario.getPopulation(), scheme, tollFactor) ) ;
		controler.setScoringFunctionFactory( scoringFunctionFactory );

		double mUTTS_hr = config.planCalcScore().getPerforming_utils_hr() - config.planCalcScore().getTraveling_utils_hr() ;
		config.planCalcScore().setMarginalUtilityOfMoney( mUTTS_hr / baseValueOfTime_h );
		System.err.println( "setting UoM in config to: " + config.planCalcScore().getMarginalUtilityOfMoney() );
		
//		config.planCalcScore().setMemorizingExperiencedPlans(true);
		
		// insert into routing:
		final ConfigurableTravelDisutilityFactory travelDisutilityFactory = new ConfigurableTravelDisutilityFactory( scenario );
		travelDisutilityFactory.setRoadPricingScheme(scheme);
//		travelDisutilityFactory.setUom(personSpecificUtilityOfMoney);
//		travelDisutilityFactory.setScoringFunctionFactory(scoringFunctionFactory);
		travelDisutilityFactory.setRandomness(10.);
		controler.setTravelDisutilityFactory( travelDisutilityFactory );
		
//		// plans removal:
//		controler.addControlerListener(new StartupListener(){
//			@Override
//			public void notifyStartup(StartupEvent event) {
//				event.getControler().getStrategyManager().setPlanSelectorForRemoval(new MyPlanSelectorForRemoval(scenario.getNetwork()));
//			}
//		});
		
		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener()) ;

		// RUN:
		controler.run();

	}

	
	public static void main( String[] args ) {
		OutputDirectoryLogging.catchLogEntries();
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
