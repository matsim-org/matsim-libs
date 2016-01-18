///* *********************************************************************** *
// * project: org.matsim.*
// * MATSim4UrbanSim.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2010 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
///**
// *
// */
//package playground.andreas.aas.modules.cellBasedAccessibility;
//
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.config.Module;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.facilities.ActivityFacilitiesImpl;
//import org.matsim.core.network.NetworkImpl;
//import org.matsim.core.network.algorithms.NetworkCleaner;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioUtils;
//
//import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
//import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimConfigurationConverterV2;
//import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimControlerConfigModule;
//import playground.tnicolai.matsim4opus.config.UrbanSimParameterConfigModule;
//import playground.tnicolai.matsim4opus.constants.InternalConstants;
//import playground.tnicolai.matsim4opus.gis.GridUtils;
//import playground.tnicolai.matsim4opus.gis.SpatialGrid;
//import playground.tnicolai.matsim4opus.gis.ZoneLayer;
//import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
//import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
//import playground.tnicolai.matsim4opus.utils.io.BackupRun;
//import playground.tnicolai.matsim4opus.utils.io.Paths;
//import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;
//import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisWorkplaceCSVWriter;
//import playground.tnicolai.matsim4opus.utils.network.NetworkBoundaryBox;
//
//import com.vividsolutions.jts.geom.Geometry;
//
//
///**
// * @author thomas
// * 
// * improvements jan'12:
// * 
// * - This class is a revised version of "MATSim4UrbanSim".
// * - Increased Configurability: 
// * 	First approach to increase the configurability of MATSim4UrbanSim modules such as
// * 	the zonz2zone impedance matrix, zone based- and grid based accessibility computation. Modules can be en-disabled
// * 	additional modules can be added by other classes extending MATSim4UrbanSimV2.
// * - Data Processing on Demand:
// *  Particular input data is processed when a corresponding module is enabled, e.g. an array of aggregated workplaces will
// *  be generated when either the zone based- or grid based accessibility computation is activated.
// * - Extensibility:
// * 	This class provides standard functionality such as configuring MATSim, reading UrbanSim input data, running the 
// * 	mobility simulation and so forth... This functionality can be extended by an inheriting class (e.g. MATSim4UrbanSimZurichAccessibility) 
// * 	by implementing certain stub methods such as "addFurtherControlerListener", "modifyNetwork", "modifyPopulation" ...
// * - Backup Results:
// *  This was also available before but not documented. Some data is overwritten with each run, e.g. the zone2zone impedance matrix or data
// *  in the MATSim output folder. If the backup is activated the most imported files (see BackupRun class) are saved in a new folder. In order 
// *  to match the saved data with the corresponding run or year the folder names contain the "simulation year" and a time stamp.
// * - Other improvements:
// * 	For a better readability of code some functionality is outsourced into helper classes
// */
//public class MATSim4UrbanSimParcel {
//
//	// logger
//	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcel.class);
//
//	// MATSim scenario
//	ScenarioImpl scenario = null;
//	// MATSim4UrbanSim configuration converter
//	MATSim4UrbanSimConfigurationConverterV2 connector = null;
//	// Reads UrbanSim Parcel output files
//	ReadFromUrbanSimModel readFromUrbansim = null;
//	// Benchmarking computation times and hard disc space ... 
//	Benchmark benchmark = null;
//	// indicates if MATSim run was successful
//	static boolean isSuccessfulMATSimRun 			 = false;
//	// needed for controler listeners
//	AggregateObject2NearestNode[] aggregatedOpportunities = null;
//	
//	boolean isParcel = true;
//	
//	// run selected controler
//	boolean computeCellBasedAccessibility			 = false;
//	boolean computeCellBasedAccessibilitiesShapeFile = false;
//	boolean computeCellBasedAccessibilitiesNetwork 	 = false; // may lead to "out of memory" error when either one/some of this is true: high resolution, huge network, less memory
//	boolean computeZoneBasedAccessibilities			 = false;
//	boolean computeZone2ZoneImpedance		   		 = false;
//	boolean computeAgentPerformance					 = false;
//	boolean dumpPopulationData 						 = false;
//	boolean dumpAggegatedWorkplaceData 			  	 = false;
//	String shapeFile 						 		 = null;
//	double cellSizeInMeter 							 = -1;
//	double destinationSampleRate 					 = 1.;
//	NetworkBoundaryBox nwBoundaryBox				 = null;
//	
//	/**
//	 * constructor
//	 * 
//	 * @param args contains at least a reference to 
//	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
//	 */
//	MATSim4UrbanSimParcel(String args[]){
//		
//		// Stores location of MATSim configuration file
//		String matsimConfiFile = (args!= null && args.length>0) ? args[0].trim():null;
//		// checks if args parameter contains a valid path
//		Paths.isValidPath(matsimConfiFile);
//		
//		if( !(connector = new MATSim4UrbanSimConfigurationConverterV2( matsimConfiFile )).init() ){
//			log.error("An error occured while initializing MATSim scenario ...");
//			System.exit(-1);
//		}
//		scenario = connector.getScenario();
//		ScenarioUtils.loadScenario(scenario);
//		setControlerSettings(scenario, args);
//		// init Benchmark as default
//		benchmark = new Benchmark();
//	}
//	
//	/**
//	 * prepare MATSim for traffic flow simulation ...
//	 */
//	@SuppressWarnings("deprecation")
//	void runMATSim(){
//		log.info("Starting MATSim from Urbansim");	
//
//		// checking if this is a test run
//		// a test run only validates the xml config file by initializing the xml config via the xsd.
//		isTestRun();
//
//		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
//		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
//		Network network = scenario.getNetwork();
//		modifyNetwork(network);
//		cleanNetwork(network);
//		
//		// get the data from UrbanSim (parcels and persons)
//		readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(), null, 0. );
//		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
//		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
//		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
//		// initializing parcels and zones from UrbanSim input
//		readUrbansimParcelModel(parcels, zones);
//		
//		// population generation
//		int pc = benchmark.addMeasure("Population construction");
//		Population newPopulation = readUrbansimPersons(parcels, network);
//		modifyPopulation(newPopulation);
//		benchmark.stoppMeasurement(pc);
//		System.out.println("Population construction took: " + benchmark.getDurationInSeconds( pc ) + " seconds.");
//
//		log.info("### DONE with demand generation from urbansim ###");
//
//		// set population in scenario
//		scenario.setPopulation(newPopulation);
//
//		// running mobsim and assigned controller listener
//		runControler(zones, parcels);
//	}
//	
//	/**
//	 * read UrbanSim parcel table and build facilities and zones in MATSim
//	 * 
//	 * @param parcels
//	 * @param zones
//	 */
//	void readUrbansimParcelModel(ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl zones){
//		readFromUrbansim.readFacilitiesParcel(parcels, zones);
//	}
//	
//	/**
//	 * reads UrbanSim persons table and creates a MATSim population
//	 * @param oldPopulation
//	 * @param parcels
//	 * @param network
//	 * @param samplingRate
//	 * @return
//	 */
//	Population readUrbanSimPopulation(Population oldPopulation, ActivityFacilitiesImpl parcels, Network network, double samplingRate){
//		return readFromUrbansim.readPersonsParcel( oldPopulation, parcels, network, samplingRate );
//	}
//	
//	/**
//	 * Reads the UrbanSim job table and aggregates jobs with same nearest node 
//	 * 
//	 * @return JobClusterObject[] 
//	 */
//	AggregateObject2NearestNode[] readUrbansimJobs(ActivityFacilitiesImpl parcels, double jobSample){
//		return readFromUrbansim.getAggregatedWorkplaces(parcels, jobSample, (NetworkImpl) scenario.getNetwork(), isParcel);
//	}
//	
//	/**
//	 * read person table from urbansim and build MATSim population
//	 * 
//	 * @param readFromUrbansim
//	 * @param parcelsOrZones
//	 * @param network
//	 * @return
//	 */
//	Population readUrbansimPersons(ActivityFacilitiesImpl parcelsOrZones, Network network){
//		// read UrbanSim population (these are simply those entities that have the person, home and work ID)
//		Population oldPopulation = null;
//		
//		MATSim4UrbanSimControlerConfigModule m4uModule= getMATSim4UrbaSimControlerConfig();
//		UrbanSimParameterConfigModule uspModule		  = getUrbanSimParameterConfig();
//		
//		
//		// check for existing plans file
//		if ( scenario.getConfig().plans().getInputFile() != null ) {
//			
//			if(m4uModule.isHotStart())
//				log.info("MATSim is running in HOT start mode, i.e. MATSim starts with pop file from previous run: " + scenario.getConfig().plans().getInputFile());
//			else if(m4uModule.isWarmStart())
//				log.info("MATSim is running in WARM start mode, i.e. MATSim starts with pre-existing pop file:" + scenario.getConfig().plans().getInputFile());
//		
//			log.info("MATSim will remove persons from plans-file, which are no longer part of the UrbanSim population!");
//			log.info("New UrbanSim persons will be added.");
//
//			oldPopulation = scenario.getPopulation() ;
//		}
//		else {
//			log.warn("No plans-file specified in the travel_model_configuration section (OPUS GUI).");
//			log.info("(MATSim is running in COLD start mode, i.e. MATSim generates new plans-file from UrbanSim input.)" );
//			oldPopulation = null;
//		}
//
//		// read UrbanSim persons. Generates hwh acts as side effect
//		Population newPopulation = readUrbanSimPopulation(oldPopulation, parcelsOrZones, network, uspModule.getPopulationSampleRate());
//		
//		// clean
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
//	void runControler( ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels){
//		
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
//		controler.setCreateGraphs(true);	// sets, whether output Graphs are created
//		
//		log.info("Adding controler listener ...");
//		addControlerListener(zones, parcels, controler);
//		addFurtherControlerListener(controler, parcels);
//		log.info("Adding controler listener done!");
//	
//		// run the iterations, including post-processing:
//		controler.run() ;
//	}
//
//	/**
//	 * The following method register listener that should be done _after_ the iterations were run.
//	 * 
//	 * @param zones
//	 * @param parcels
//	 * @param controler
//	 */
//	void addControlerListener(ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, Controler controler) {
//		
//		// set spatial reference id (not necessary but needed to match the outcomes with google maps)
//		int srid = InternalConstants.SRID_SWITZERLAND; // Constants.SRID_WASHINGTON_NORTH
//
//		// The following lines register what should be done _after_ the iterations are done:
//		if(computeZone2ZoneImpedance)
//			// creates zone2zone impedance matrix
//			controler.addControlerListener( new Zone2ZoneImpedancesControlerListener( zones, 
//																					  parcels,
//																					  benchmark) );
//		if(computeAgentPerformance)
//			// creates a persons.csv output for UrbanSim
//			controler.addControlerListener(new AgentPerformanceControlerListener(benchmark));
//		
//		if(computeZoneBasedAccessibilities){
//			
//			ZoneLayer<Id>  measuringPoints = GridUtils.convertActivityFacilities2ZoneLayer(zones, srid);
//			
//			// init aggregatedWorkplaces
//			if(aggregatedOpportunities == null)
//				aggregatedOpportunities = readUrbansimJobs(parcels, destinationSampleRate);
//			// creates zone based table of log sums (workplace accessibility)
//			controler.addControlerListener( new ZoneBasedAccessibilityControlerListenerV3(measuringPoints, 				
//																						aggregatedOpportunities, 
//																						benchmark,
//																						this.scenario));
//		}
//		
//		// new method
//		if(computeCellBasedAccessibility){
//			SpatialGrid freeSpeedGrid;				// matrix for free speed car related accessibility measure. based on the boundary (above) and grid size
//			SpatialGrid carGrid;					// matrix for congested car related accessibility measure. based on the boundary (above) and grid size
//			SpatialGrid bikeGrid;					// matrix for bike related accessibility measure. based on the boundary (above) and grid size
//			SpatialGrid walkGrid;					// matrix for walk related accessibility measure. based on the boundary (above) and grid size
//			
//			ZoneLayer<Id>  measuringPoints;
//			String fileExtension;
//			
//			// aggregate destinations (opportunities) on the nearest node on the road network to speed up accessibility computation
//			if(aggregatedOpportunities == null)
//				aggregatedOpportunities = readUrbansimJobs(parcels, destinationSampleRate);
//			
//			if(computeCellBasedAccessibilitiesNetwork){
//				fileExtension = CellBasedAccessibilityControlerListenerV2.NETWORK;
//				measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(cellSizeInMeter, 
//																			   nwBoundaryBox.getBoundingBox(),
//																			   srid);
//				freeSpeedGrid= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
//				carGrid = new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
//				bikeGrid = new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
//				walkGrid= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
//			}
//			else{
//				fileExtension = CellBasedAccessibilityControlerListenerV2.SHAPE_FILE;
//				Geometry boundary = GridUtils.getBoundary(shapeFile, srid);
//				measuringPoints   = GridUtils.createGridLayerByGridSizeByShapeFile(cellSizeInMeter, 
//																				   boundary, 
//																				   srid);
//				freeSpeedGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
//				carGrid	= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
//				bikeGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
//				walkGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
//			}
//			
//			controler.addControlerListener(new CellBasedAccessibilityControlerListenerV3(measuringPoints, 
//																						 aggregatedOpportunities,
//																						 parcels,
//																						 freeSpeedGrid,
//																						 carGrid,
//																						 bikeGrid,
//																						 walkGrid, 
//																						 fileExtension, 
//																						 benchmark, 
//																						 this.scenario));
//		}
//		
//		if(dumpPopulationData)
//			readFromUrbansim.readAndDumpPersons2CSV(parcels, 
//												 	controler.getNetwork());
//		
//		if(dumpAggegatedWorkplaceData){
//			// init aggregatedWorkplaces
//			if(aggregatedOpportunities == null)
//				aggregatedOpportunities = readUrbansimJobs(parcels, destinationSampleRate);
//			AnalysisWorkplaceCSVWriter.writeAggregatedWorkplaceData2CSV(aggregatedOpportunities);
//		}
//		
//		// to count number of cars per h on a link
//		// write ControlerListener that implements AfterMobsimListener (notifyAfterMobsim)
//		// get VolumeLinkAnalyzer by "event.getServices.getVolume... and run getVolumesForLink. that returns an int array with the number of cars per hour on an specific link
//		// see also http://matsim.org/docs/controler
//	}
//	
//	/**
//	 * This method allows to add additional listener
//	 * This needs to be implemented by another class
//	 */
//	void addFurtherControlerListener(Controler controler, ActivityFacilitiesImpl parcels){
//		// this is just a stub and does nothing. 
//		// This needs to be implemented/overwritten by an inherited class
//	}
//	
//	void setControlerSettings(ScenarioImpl scenario, String[] args) {
//
//		AccessibilityParameterConfigModule moduleAccessibility = getAccessibilityParameterConfig();
//		MATSim4UrbanSimControlerConfigModule moduleMATSim4UrbanSim = getMATSim4UrbaSimControlerConfig();
//		
//		this.destinationSampleRate 		= moduleAccessibility.getAccessibilityDestinationSamplingRate();
//
//		this.computeAgentPerformance	= moduleMATSim4UrbanSim.isAgentPerformance();
//		this.computeZone2ZoneImpedance	= moduleMATSim4UrbanSim.isZone2ZoneImpedance();
//		this.computeZoneBasedAccessibilities = moduleMATSim4UrbanSim.isZoneBasedAccessibility();
//		this.computeCellBasedAccessibility	= moduleMATSim4UrbanSim.isCellBasedAccessibility();
//		this.computeCellBasedAccessibilitiesShapeFile = moduleMATSim4UrbanSim.isCellBasedAccessibilityShapeFile();
//		this.computeCellBasedAccessibilitiesNetwork = moduleMATSim4UrbanSim.isCellBasedAccessibilityNetwork();
//		this.dumpPopulationData 		= false;
//		this.dumpAggegatedWorkplaceData = true;
//		
//		this.cellSizeInMeter 			= moduleMATSim4UrbanSim.getCellSizeCellBasedAccessibility();
//		this.shapeFile					= moduleMATSim4UrbanSim.getShapeFileCellBasedAccessibility();
//		// using custom bounding box, defining the study area for accessibility computation
//		this.nwBoundaryBox 				= new NetworkBoundaryBox();
//		if(moduleMATSim4UrbanSim.isUseCustomBoundingBox()){
//			nwBoundaryBox.setCustomBoundaryBox(moduleMATSim4UrbanSim.getBoundingBoxLeft(), 
//													moduleMATSim4UrbanSim.getBoundingBoxBottom(), 
//													moduleMATSim4UrbanSim.getBoundingBoxRight(), 
//													moduleMATSim4UrbanSim.getBoundingBoxTop());
//		}
//		// using boundary of hole network for accessibility computation
//		else{
//			log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
//			nwBoundaryBox.setDefaultBoundaryBox(scenario.getNetwork());
//		}
//	}
//
//	/**
//	 * cleaning matsim network
//	 * @param network
//	 */
//	void cleanNetwork(Network network){
//		log.info("") ;
//		log.info("Cleaning network ...");
//		(new NetworkCleaner() ).run(network);
//		log.info("... finished cleaning network.");
//		log.info("");
//		// (new NetworkRemoveUnusedNodes()).run(network); // tnicolai feb'12 not necessary for ivtch-network
//	}
//	
//	/**
//	 * This method allows to modify the MATSim network
//	 * This needs to be implemented by another class
//	 * 
//	 * @param network
//	 */
//	void modifyNetwork(Network network){
//		// this is just a stub and does nothing. 
//		// This needs to be implemented/overwritten by an inherited class
//	}
//	
//	/**
//	 * This method allows to modify the population
//	 * This needs to be implemented by another class
//	 * 
//	 * @param population
//	 */
//	void modifyPopulation(Population population){
//		// this is just a stub and does nothing. 
//		// This needs to be implemented/overwritten by an inherited class
//	}
//	
//	/**
//	 * triggers backup of MATSim and UrbanSim Output
//	 */
//	void matim4UrbanSimShutdown(){
//		BackupRun.runBackup(scenario);
//	}
//	
//	/**
//	 * 
//	 */
//	void isTestRun(){
//		if(getUrbanSimParameterConfig().isTestRun()){
//			log.info("TestRun was successful...");
//			MATSim4UrbanSimParcel.isSuccessfulMATSimRun = true;
//			return;
//		}
//	}
//	
//	AccessibilityParameterConfigModule getAccessibilityParameterConfig() {
//		Module m = this.scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
//		if (m instanceof AccessibilityParameterConfigModule) {
//			return (AccessibilityParameterConfigModule) m;
//		}
//		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
//		this.scenario.getConfig().getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
//		return apcm;
//	}
//	
//	MATSim4UrbanSimControlerConfigModule getMATSim4UrbaSimControlerConfig() {
//		Module m = this.scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
//		if (m instanceof MATSim4UrbanSimControlerConfigModule) {
//			return (MATSim4UrbanSimControlerConfigModule) m;
//		}
//		MATSim4UrbanSimControlerConfigModule mccm = new MATSim4UrbanSimControlerConfigModule(MATSim4UrbanSimControlerConfigModule.GROUP_NAME);
//		this.scenario.getConfig().getModules().put(MATSim4UrbanSimControlerConfigModule.GROUP_NAME, mccm);
//		return mccm;
//	}
//	
//	UrbanSimParameterConfigModule getUrbanSimParameterConfig() {
//		Module m = this.scenario.getConfig().getModule(UrbanSimParameterConfigModule.GROUP_NAME);
//		if (m instanceof UrbanSimParameterConfigModule) {
//			return (UrbanSimParameterConfigModule) m;
//		}
//		UrbanSimParameterConfigModule upcm = new UrbanSimParameterConfigModule(UrbanSimParameterConfigModule.GROUP_NAME);
//		this.scenario.getConfig().getModules().put(UrbanSimParameterConfigModule.GROUP_NAME, upcm);
//		return upcm;
//	}
//	
//	/**
//	 * Entry point
//	 * @param args UrbanSim command prompt
//	 */
//	public static void main(String args[]){
//		MATSim4UrbanSimParcel m4u = new MATSim4UrbanSimParcel(args);
//		m4u.runMATSim();
//		m4u.matim4UrbanSimShutdown();
//		MATSim4UrbanSimParcel.isSuccessfulMATSimRun = Boolean.TRUE;
//	}
//	
//	/**
//	 * this method is only called/needed by "matsim4opus.matsim.MATSim4UrbanSimTest"
//	 * @return true if run was successful
//	 */
//	public static boolean getRunStatus(){
//		return MATSim4UrbanSimParcel.isSuccessfulMATSimRun;
//	}
//}
