package tryouts.tests;
//package playground.tnicolai.urbansim.tests;
//
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.facilities.ActivityFacilitiesImpl;
//import org.matsim.core.network.NetworkImpl;
//import org.matsim.core.population.PopulationWriter;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.misc.ConfigUtils;
//
//import playground.tnicolai.urbansim.constants.Constants;
//import playground.tnicolai.urbansim.matsim4urbansim.MATSim4Urbansim;
//import playground.tnicolai.urbansim.utils.helperObjects.WorkplaceObject;
//import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
//
//public class TestWarmStart extends MATSim4Urbansim{
//
//	// logger
//	private static final Logger log = Logger.getLogger(TestWarmStart.class);
//	
//	private int dummyYear = -1;
//	
//	public TestWarmStart(String args[]){
//		super(args);
//		
//		dummyYear = Integer.parseInt(args[1]);
//	}
//	
//	@Override
//	public void runMATSim(){
//		
//		log.info("Starting MATSim from Urbansim");
//
//		// checking for if this is only a test run
//		// a test run only validates the xml config file by initializing the xml config via the xsd.
//		if(scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.IS_TEST_RUN).equalsIgnoreCase(Constants.TRUE)){
//			log.info("TestRun was successful...");
//			return;
//		}
//
//		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
//		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
//		NetworkImpl network = scenario.getNetwork();
//		cleanNetwork(network);
//		
//		// get the data from urbansim (parcels and persons)
//		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( dummyYear, benchmark );
//		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
//		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
//		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
//		
//		readUrbansimParcelModel(readFromUrbansim, facilities, zones);
//		Population newPopulation = readUrbansimPersons(readFromUrbansim, facilities, network);
//		Map<Id,WorkplaceObject> numberOfWorkplacesPerZone = ReadUrbansimJobs(readFromUrbansim);
//		
//		log.info("### DONE with demand generation from urbansim ###") ;
//
//		// set population in scenario
//		scenario.setPopulation(newPopulation);
//
//		runControler(zones, numberOfWorkplacesPerZone, facilities, readFromUrbansim);
//	}
//	
//	/**
//	 * read person table from urbansim and build MATSim population
//	 * 
//	 * @param readFromUrbansim
//	 * @param parcels
//	 * @param network
//	 * @return
//	 */
//	@Override
//	protected Population readUrbansimPersons(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl parcels, NetworkImpl network){
//		// read urbansim population (these are simply those entities that have the person, home and work ID)
//		Population oldPopulation = null;
//		if ( scenario.getConfig().plans().getInputFile() != null ) {
//			log.info("Population specified in matsim config file; assuming WARM start with pre-existing pop file.");
//			log.info("Persons not found in pre-existing pop file are added; persons no longer in urbansim persons file are removed." ) ;
//			oldPopulation = scenario.getPopulation() ;
//			log.info("Note that the `continuation of iterations' will only work if you set this up via different config files for") ;
//			log.info(" every year and know what you are doing.") ;
//		}
//		else {
//			log.warn("No population specified in matsim config file; assuming COLD start.");
//			log.info("(I.e. generate new pop from urbansim files.)" );
//			oldPopulation = null;
//		}
//
//		Population newPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
//		// read urbansim persons.  Generates hwh acts as side effect
//		readFromUrbansim.readPersons( oldPopulation, newPopulation, parcels, network, Double.parseDouble( scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.SAMPLING_RATE)) ) ;
//		oldPopulation=null;
//		System.gc();
//		
//		return newPopulation;
//	}
//	
//	/**
//	 * run simulation
//	 * @param zones
//	 */
//	@Override
//	protected void runControler( ActivityFacilitiesImpl zones, Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl parcels, 
//			ReadFromUrbansimParcelModel readFromUrbansim){
//		
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
//		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
//		
//		// The following lines register what should be done _after_ the iterations were run:
////		controler.addControlerListener( new MyControlerListener( zones, numberOfWorkplacesPerZone, parcels, scenario ) );
//		
//		// run the iterations, including the post-processing:
//		controler.run();
//	}
//	
//	/**
//	 * Entry point
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		
//		final String matsimConfig = "/Users/thomas/Development/opus_home/opus_matsim/matsim_config/test_derived_from_seattle_parcel_matsim_config.xml";
//		int year = 2001;
//		
//		for(int i = 0; i < 5; i++){
//			
//			String myArgs[] = new String[]{matsimConfig, year+""};
//		
//			TestWarmStart testWS = new TestWarmStart(myArgs);
//			testWS.runMATSim();
//			
//			year++;
//		}
//	}
//	
//}
