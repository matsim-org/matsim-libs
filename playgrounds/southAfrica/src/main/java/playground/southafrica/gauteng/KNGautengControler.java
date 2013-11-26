package playground.southafrica.gauteng;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.analysis.kai.KaiAnalysisListener;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.gauteng.roadpricingscheme.GautengRoadPricingScheme;
import playground.southafrica.gauteng.routing.AutosensingTravelDisutilityInclTollFactory;
import playground.southafrica.gauteng.routing.GautengTravelDisutilityInclTollFactory;
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

	final static String outputDirectoryName = "/Users/nagel/kairuns/output2/";
	
	void run ( String[] args ) {
		Header.printHeader(KNGautengControler.class.toString(), args);

		// === CONFIG (we now suggest to build config, scenario, controler sequentially): ===
		Config config = null ;
		String tollFilename = null ;
		Double baseValueOfTime = 1. ; // yy unit = ??
		Double valueOfTimeMultiplier = 4. ; 
		if ( args!=null && args.length>0 && args[0] != null ) {
			if(args.length != 6){
				throw new RuntimeException("Must provide the following arguments: config file path; " +
						"input plans file; road pricing file to use; base value of time (for cars); " +
						"multiplier for commercial vehicles; and number of threads to use (globally).") ;
			}
			// the following way to do this is not better than it was before, but shorter, which is what I want here. kai, nov'13
			config = ConfigUtils.loadConfig(args[0]) ;
			config.plans().setInputFile(args[1]) ;
			tollFilename = args[2];

			// Read the base Value-of-Time (VoT) for private cars, and the VoT multiplier from the arguments, johan Mar'12
			baseValueOfTime = Double.parseDouble(args[3]);
			valueOfTimeMultiplier = Double.parseDouble(args[4]);
			config.global().setNumberOfThreads(Integer.parseInt(args[5]));
		} else {
			config = ConfigUtils.loadConfig("/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/config/kaiconfig.xml") ;
			config.network().setInputFile("/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/network/gautengNetwork_CleanV2.xml.gz");
			config.plans().setInputFile("/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/plans/car-com-bus-taxi-ext_plans_2009_1pct-with-routesV0.xml.gz") ;
			
			tollFilename = "/Users/nagel/ie-calvin/MATSim-SA/trunk/data/sanral2010/toll/gauteng_toll_joint_weekday_02.xml" ;

			config.controler().setMobsim( MobsimType.JDEQSim.toString() );
			config.controler().setRoutingAlgorithmType( RoutingAlgorithmType.FastAStarLandmarks );
			config.controler().setOutputDirectory(outputDirectoryName);
			config.controler().setLastIteration(1);
			
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.FLOW_CAPACITY_FACTOR, "0.01") ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.STORAGE_CAPACITY_FACTOR, "0.03" ) ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.CAR_SIZE, "7.5" ) ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.END_TIME, "36:00:00" ) ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.GAP_TRAVEL_SPEED, "15.0" ) ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.MINIMUM_INFLOW_CAPACITY, "1800" ) ;
			config.setParam( JDEQSimulation.JDEQ_SIM, JDEQSimulation.SQUEEZE_TIME, "5" ) ;

			config.global().setNumberOfThreads(1);
		}
		
		if (config.scenario().isUseRoadpricing()) {
			throw new RuntimeException("roadpricing must NOT be enabled in config.scenario in order to use special " +
					"road pricing features.  aborting ...");
		}
				
		


		// CONSTRUCT UTILITY OF MONEY:
		UtilityOfMoneyI personSpecificUtilityOfMoney = 
				new GautengUtilityOfMoney( config.planCalcScore() , baseValueOfTime, valueOfTimeMultiplier) ;

		// === SCENARIO: ===
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		// CONSTRUCT VEH-DEP ROAD PRICING SCHEME:
		RoadPricingScheme vehDepScheme = 
				new GautengRoadPricingScheme( tollFilename, scenario.getNetwork() , scenario.getPopulation() );

		final GautengScoringFunctionFactory scoringFunctionFactory = 
				new GautengScoringFunctionFactory(scenario.getConfig(), scenario.getNetwork(), personSpecificUtilityOfMoney );

		// === CONTROLER: ===

		final Controler controler = new Controler( scenario ) ;
		controler.setOverwriteFiles(true) ;

		// INSTALL ROAD PRICING (in the longer run, re-merge with RoadPricing class):
		// insert into scoring:
		controler.addControlerListener(
				new GenerationOfMoneyEvents( scenario.getNetwork(), scenario.getPopulation(), vehDepScheme) 
				) ;
		controler.setScoringFunctionFactory( scoringFunctionFactory );

		// insert into routing:
		controler.setTravelDisutilityFactory( 
				new GautengTravelDisutilityInclTollFactory( vehDepScheme, personSpecificUtilityOfMoney ) 
				);
//		controler.setTravelDisutilityFactory( 
//				new AutosensingTravelDisutilityInclTollFactory(vehDepScheme, scenario, scoringFunctionFactory) 
//				);
		

		// ADDITIONAL ANALYSIS:
		controler.addControlerListener(new KaiAnalysisListener()) ;

		// RUN:
		controler.run();

	}
	
	public static void main( String[] args ) {
		new KNGautengControler().run( args ) ;
	}
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public final void testOne() {
		new KNGautengControler().run( null ) ;
		
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
