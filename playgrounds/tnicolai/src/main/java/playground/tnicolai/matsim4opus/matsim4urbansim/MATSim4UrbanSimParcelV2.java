/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */
package playground.tnicolai.matsim4opus.matsim4urbansim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimControlerConfigModule;
import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimConfigurationConverterV2;
import playground.tnicolai.matsim4opus.config.UrbanSimParameterConfigModule;
import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.io.BackupRun;
import playground.tnicolai.matsim4opus.utils.io.Paths;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
import playground.tnicolai.matsim4opus.utils.network.NetworkBoundaryBox;


/**
 * @author thomas
 * 
 * improvements jan'12:
 * 
 * - This class is a revised version of "MATSim4UrbanSim".
 * - Increased Configurability: 
 * 	First approach to increase the configurability of MATSim4UrbanSim modules such as
 * 	the zonz2zone impedance matrix, zone based- and grid based accessibility computation. Modules can be en-disabled
 * 	additional modules can be added by other classes extending MATSim4UrbanSimV2.
 * - Data Processing on Demand:
 *  Particular input data is processed when a corresponding module is enabled, e.g. an array of aggregated workplaces will
 *  be generated when either the zone based- or grid based accessibility computation is activated.
 * - Extensibility:
 * 	This class provides standard functionality such as configuring MATSim, reading UrbanSim input data, running the 
 * 	mobility simulation and so forth... This functionality can be extended by an inheriting class (e.g. MATSim4UrbanSimZurichAccessibility) 
 * 	by implementing certain stub methods such as "addFurtherControlerListener", "modifyNetwork", "modifyPopulation" ...
 * - Backup Results:
 *  This was also available before but not documented. Some data is overwritten with each run, e.g. the zone2zone impedance matrix or data
 *  in the MATSim output folder. If the backup is activated the most imported files (see BackupRun class) are saved in a new folder. In order 
 *  to match the saved data with the corresponding run or year the folder names contain the "simulation year" and a time stamp.
 * - Other improvements:
 * 	For a better readability of code some functionality is outsourced into helper classes
 */
public class MATSim4UrbanSimParcelV2 {

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcelV2.class);

	// MATSim scenario
	ScenarioImpl scenario = null;
	// MATSim4UrbanSim configuration converter
	MATSim4UrbanSimConfigurationConverterV2 connector = null;
	// Reads UrbanSim Parcel output files
	ReadFromUrbansimParcelModel readFromUrbansim = null;
	// Benchmarking computation times and hard disc space ... 
	Benchmark benchmark = null;
	// indicates if MATSim run was successful
	static boolean isSuccessfulMATSimRun 			 = false;
	// needed for controler listeners
	AggregateObject2NearestNode[] aggregatedOpportunities = null;
	
	// run selected controler
	boolean computeCellBasedAccessibilitiesShapeFile = false;
	boolean computeZone2ZoneImpedance		   		 = false;
	boolean computeCellBasedAccessibilitiesNetwork 	 = false; // may lead to "out of memory" error when either one/some of this is true: high resolution, huge network, less memory
	boolean computeZoneBasedAccessibilities			 = false;
	boolean computeAgentPerformance					 = false;
	boolean dumpPopulationData 						 = false;
	boolean dumpAggegatedWorkplaceData 			  	 = false;
	String shapeFile 						 		 = null;
	double cellSizeInMeter 							 = -1;
	double jobSampleRate 							 = 1.;
	NetworkBoundaryBox nwBoundaryBox				 = null;
	
	/**
	 * constructor
	 * 
	 * @param args contains at least a reference to 
	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
	 */
	MATSim4UrbanSimParcelV2(String args[]){
		
		// Stores location of MATSim configuration file
		String matsimConfiFile = (args!= null && args.length>0) ? args[0].trim():null;
		// checks if args parameter contains a valid path
		Paths.isValidPath(matsimConfiFile);
		
		if( !(connector = new MATSim4UrbanSimConfigurationConverterV2( matsimConfiFile )).init() ){
			log.error("An error occured while initializing MATSim scenario ...");
			System.exit(-1);
		}
		scenario = connector.getScenario();
		ScenarioUtils.loadScenario(scenario);
		setControlerSettings(scenario, args);
		// init Benchmark as default
		benchmark = new Benchmark();
	}
	
	/**
	 * prepare MATSim for traffic flow simulation ...
	 */
	void runMATSim(){
		log.info("Starting MATSim from Urbansim");	

		// checking if this is a test run
		// a test run only validates the xml config file by initializing the xml config via the xsd.
		isTestTun();

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		Network network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);
		
		// get the data from UrbanSim (parcels and persons)
		readFromUrbansim = new ReadFromUrbansimParcelModel( getUrbanSimParameterConfig().getYear() );
		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		// initializing parcels and zones from UrbanSim input
		readUrbansimParcelModel(parcels, zones);
		
		// population generation
		int pc = benchmark.addMeasure("Population construction");
		Population newPopulation = readUrbansimPersons(parcels, network);
		modifyPopulation(newPopulation);
		benchmark.stoppMeasurement(pc);
		System.out.println("Population construction took: " + benchmark.getDurationInSeconds( pc ) + " seconds.");

		log.info("### DONE with demand generation from urbansim ###");

		// set population in scenario
		scenario.setPopulation(newPopulation);

		// running mobsim and assigned controller listener
		runControler(zones, parcels);
	}
	
	void isTestTun(){
//		if( scenario.getConfig().getParam(Constants.URBANSIM_PARAMETER, Constants.IS_TEST_RUN).equalsIgnoreCase(Constants.TRUE)){
//			log.info("TestRun was successful...");
//			return;
//		}
		if(getUrbanSimParameterConfig().isTestRun()){
			log.info("TestRun was successful...");
			return;
		}
	}
	
	/**
	 * read UrbanSim parcel table and build facilities and zones in MATSim
	 * 
	 * @param parcels
	 * @param zones
	 */
	void readUrbansimParcelModel(ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl zones){
		readFromUrbansim.readFacilities(parcels, zones);
	}
	
	/**
	 * reads UrbanSim persons table and creates a MATSim population
	 * @param oldPopulation
	 * @param parcels
	 * @param network
	 * @param samplingRate
	 * @return
	 */
	Population readUrbanSimPopulation(Population oldPopulation, ActivityFacilitiesImpl parcels, Network network, double samplingRate){
		return readFromUrbansim.readPersons( oldPopulation, parcels, network, samplingRate );
	}
	
	/**
	 * Reads the UrbanSim job table and aggregates jobs with same nearest node 
	 * 
	 * @return JobClusterObject[] 
	 */
	AggregateObject2NearestNode[] readUrbansimJobs(ActivityFacilitiesImpl parcels, double jobSample){
		return readFromUrbansim.getAggregatedWorkplaces(parcels, jobSample, (NetworkImpl) scenario.getNetwork());
	}
	
	/**
	 * read person table from urbansim and build MATSim population
	 * 
	 * @param readFromUrbansim
	 * @param parcels
	 * @param network
	 * @return
	 */
	Population readUrbansimPersons(ActivityFacilitiesImpl parcels, Network network){
		// read UrbanSim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;
		
		MATSim4UrbanSimControlerConfigModule m4uModule= getMATSim4UrbaSimControlerConfig();
		UrbanSimParameterConfigModule uspModule		  = getUrbanSimParameterConfig();
		
		
		// check for existing plans file
		if ( scenario.getConfig().plans().getInputFile() != null ) {
			
			if(m4uModule.isHotStart())
				log.info("MATSim is running in HOT start mode, i.e. MATSim starts with pop file from previous run: " + scenario.getConfig().plans().getInputFile());
			else if(m4uModule.isWarmStart())
				log.info("MATSim is running in WARM start mode, i.e. MATSim starts with pre-existing pop file:" + scenario.getConfig().plans().getInputFile());
		
			log.info("MATSim will remove persons from plans-file, which are no longer part of the UrbanSim population!");
			log.info("New UrbanSim persons will be added.");

			oldPopulation = scenario.getPopulation() ;
		}
		else {
			log.warn("No plans-file specified in the travel_model_configuration section (OPUS GUI).");
			log.info("(MATSim is running in COL start mode, i.e. MATSim generates new plans-file from UrbanSim input.)" );
			oldPopulation = null;
		}

		// read UrbanSim persons.  Generates hwh acts as side effect
		Population newPopulation = readUrbanSimPopulation(oldPopulation,parcels, network, uspModule.getPopulationSampleRate());//readFromUrbansim.readPersons( oldPopulation, parcels, network, uspModule.getPopulationSampleRate() ) ;
		
		// clean
		oldPopulation=null;
		System.gc();
		
		return newPopulation;
	}
	
	/**
	 * run simulation
	 * @param zones
	 */
	void runControler( ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels){
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		log.info("Adding controler listener ...");
		addControlerListener(zones, parcels, controler);
		addFurtherControlerListener(controler, parcels);
		log.info("Adding controler listener done!");
		
		// tnicolai todo?: count number of cars per h on a link
		// write ControlerListener that implements AfterMobsimListener (notifyAfterMobsim)
		// get VolumeLinkAnalyzer by "event.getControler.getVolume... and run getVolumesForLink. that returns an int array with the number of cars per hour on an specific link 
		// see also http://matsim.org/docs/controler
		
		// run the iterations, including post-processing:
		controler.run() ;
	}

	/**
	 * The following method register listener that should be done _after_ the iterations were run.
	 * 
	 * @param zones
	 * @param parcels
	 * @param controler
	 */
	void addControlerListener(ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, Controler controler) {

		// The following lines register what should be done _after_ the iterations are done:
		if(computeZone2ZoneImpedance)
			// creates zone2zone impedance matrix
			controler.addControlerListener( new Zone2ZoneImpedancesControlerListener( zones, 
																					  parcels) );
		if(computeAgentPerformance)
			// creates a persons.csv output for UrbanSim
			controler.addControlerListener(new AgentPerformanceControlerListener(benchmark));
		
		if(computeZoneBasedAccessibilities){
			
			// init aggregatedWorkplaces
			if(aggregatedOpportunities == null)
				aggregatedOpportunities = readUrbansimJobs(parcels, jobSampleRate);
			// creates zone based table of log sums (workplace accessibility)
			// uses always a 100% jobSample size (see readUrbansimJobs below)
			controler.addControlerListener( new ZoneBasedAccessibilityControlerListener(zones, 				
																						aggregatedOpportunities, 
																						benchmark));
		}
		
		if(dumpPopulationData)
			readFromUrbansim.readAndDumpPersons2CSV(parcels, 
												 	controler.getNetwork());
		
		if(dumpAggegatedWorkplaceData){
			// init aggregatedWorkplaces
			if(aggregatedOpportunities == null)
				aggregatedOpportunities = readUrbansimJobs(parcels, jobSampleRate);
			AnalysisWorkplaceCSVWriter.writeAggregatedWorkplaceData2CSV(Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv", 
																		aggregatedOpportunities);
		}
	}
	
	/**
	 * This method allows to add additional listener
	 * This needs to be implemented by another class
	 */
	void addFurtherControlerListener(Controler controler, ActivityFacilitiesImpl parcels){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by an inherited class
	}
	
	void setControlerSettings(ScenarioImpl scenario, String[] args) {
		// setting workplace/job sample rate
		checkAndSetJobSample(args); // tnicolai make configurable, use opportunitySamplingRate in MATSim4UrbaSimControlerConfigModule

		MATSim4UrbanSimControlerConfigModule module = getMATSim4UrbaSimControlerConfig();

		this.computeAgentPerformance	= module.isAgentPerformance();
		this.computeZone2ZoneImpedance	= module.isZone2ZoneImpedance();
		this.computeZoneBasedAccessibilities = module.isZoneBasedAccessibility();
		this.computeCellBasedAccessibilitiesShapeFile = module.isCellBasedAccessibilityShapeFile();
		this.computeCellBasedAccessibilitiesNetwork = module.isCellBasedAccessibilityNetwork();
		this.dumpPopulationData = false;
		this.dumpAggegatedWorkplaceData = true;
		
		this.cellSizeInMeter 			= module.getCellSizeCellBasedAccessibility();
		this.shapeFile					= module.getShapeFileCellBasedAccessibility();
		// using custom bounding box, defining the study area for accessibility computation
		this.nwBoundaryBox 				= new NetworkBoundaryBox();
		if(module.isUseCustomBoundingBox()){
			nwBoundaryBox.setCustomBoundaryBox(module.getBoundingBoxLeft(), 
													module.getBoundingBoxBottom(), 
													module.getBoundingBoxRight(), 
													module.getBoundingBoxTop());
		}
		// using boundary of hole network for accessibility computation
		else{
			log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
			nwBoundaryBox.setDefaultBoundaryBox(scenario.getNetwork());
		}
	}
	
	/**
	 * Set the jobSample for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetJobSample(String[] args) {

		if (args.length >= 2) 
			jobSampleRate = Double.parseDouble(args[1].trim());
		log.info("The jobSampleRate is set to " + String.valueOf(jobSampleRate));
	}
	
	/**
	 * cleaning matsim network
	 * @param network
	 */
	void cleanNetwork(Network network){
		log.info("") ;
		log.info("Cleaning network ...");
		(new NetworkCleaner() ).run(network);
		log.info("... finished cleaning network.");
		log.info("");
		// (new NetworkRemoveUnusedNodes()).run(network); // tnicolai feb'12 not necessary for ivtch-network
	}
	
	/**
	 * This method allows to modify the MATSim network
	 * This needs to be implemented by another class
	 * 
	 * @param network
	 */
	void modifyNetwork(Network network){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by an inherited class
	}
	
	/**
	 * This method allows to modify the population
	 * This needs to be implemented by another class
	 * 
	 * @param population
	 */
	void modifyPopulation(Population population){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by an inherited class
	}
	
	/**
	 * triggers backup of MATSim and UrbanSim Output
	 */
	void matim4UrbanSimShutdown(){
		BackupRun.runBackup(scenario);
	}
	
	AccessibilityParameterConfigModule getAccessibilityParameterConfig() {
		Module m = this.scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
		return apcm;
	}
	
	MATSim4UrbanSimControlerConfigModule getMATSim4UrbaSimControlerConfig() {
		Module m = this.scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModule) {
			return (MATSim4UrbanSimControlerConfigModule) m;
		}
		MATSim4UrbanSimControlerConfigModule mccm = new MATSim4UrbanSimControlerConfigModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(MATSim4UrbanSimControlerConfigModule.GROUP_NAME, mccm);
		return mccm;
	}
	
	UrbanSimParameterConfigModule getUrbanSimParameterConfig() {
		Module m = this.scenario.getConfig().getModule(UrbanSimParameterConfigModule.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModule) {
			return (UrbanSimParameterConfigModule) m;
		}
		UrbanSimParameterConfigModule upcm = new UrbanSimParameterConfigModule(UrbanSimParameterConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(UrbanSimParameterConfigModule.GROUP_NAME, upcm);
		return upcm;
	}
	
	/**
	 * Entry point
	 * @param args UrbanSim command prompt
	 */
	public static void main(String args[]){
		MATSim4UrbanSimParcelV2 m4u = new MATSim4UrbanSimParcelV2(args);
		m4u.runMATSim();
		m4u.matim4UrbanSimShutdown();
		MATSim4UrbanSimParcelV2.isSuccessfulMATSimRun = Boolean.TRUE;
	}
	
	/**
	 * this method is only called/needed by "matsim4opus.matsim.MATSim4UrbanSimTest"
	 * @return true if run was successful
	 */
	public static boolean getRunStatus(){
		return MATSim4UrbanSimParcelV2.isSuccessfulMATSimRun;
	}
}
