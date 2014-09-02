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


import com.vividsolutions.jts.geom.Geometry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;
import org.matsim.roadpricing.RoadPricingConfigGroup;

import playground.tnicolai.matsim4opus.config.AccessibilityParameterConfigModule;
import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimConfigurationConverterV4;
import playground.tnicolai.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import playground.tnicolai.matsim4opus.config.UrbanSimParameterConfigModuleV3;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.io.BackupMATSimOutput;
import playground.tnicolai.matsim4opus.utils.io.Paths;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbanSimModel;
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
public class MATSim4UrbanSimParcel implements MATSim4UrbanSimInterface{

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcel.class);

	// MATSim scenario
	ScenarioImpl scenario = null;
	// MATSim4UrbanSim configuration converter
	MATSim4UrbanSimConfigurationConverterV4 connector = null;
	// Reads UrbanSim Parcel output files
	ReadFromUrbanSimModel readFromUrbansim = null;
	// Benchmarking computation times and hard disc space ... 
	Benchmark benchmark = null;
	// indicates if MATSim run was successful
	static boolean isSuccessfulMATSimRun 			 = false;
	// needed for controler listeners
	AggregateObject2NearestNode[] aggregatedOpportunities = null;
	
	boolean isParcelMode = true;
	
	double timeOfDay	 = -1.;
	
	// run selected controler
	boolean computeParcelBasedAccessibility			 = false;
	boolean computeParcelBasedAccessibilitiesShapeFile= false;
	boolean computeParcelBasedAccessibilitiesNetwork = false; // may lead to "out of memory" error when either one/some of this is true: high resolution, huge network, less memory
	boolean computeZoneBasedAccessibilities			 = false;
	boolean computeZone2ZoneImpedance		   		 = false;
	boolean computeAgentPerformance					 = false;
	boolean dumpPopulationData 						 = false;
	boolean dumpAggegatedWorkplaceData 			  	 = false;
	String shapeFile 						 		 = null;
	double cellSizeInMeter 							 = -1;
	double opportunitySampleRate 					 = 1.;
	NetworkBoundaryBox nwBoundaryBox				 = null;
	
	/**
	 * constructor
	 * 
	 * @param args contains at least a reference to 
	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
	 */
	MATSim4UrbanSimParcel(String args[]){
		
		// Stores location of MATSim configuration file
		String matsimConfiFile = (args!= null && args.length==1) ? args[0].trim():null;
		// checks if args parameter contains a valid path
		Paths.isValidPath(matsimConfiFile);
		
		if( !(connector = new MATSim4UrbanSimConfigurationConverterV4( matsimConfiFile )).init() ){
			log.error("An error occured while initializing MATSim scenario ...");
			System.exit(-1);
		}
		
		Config config = connector.getConfig();
		scenario = (ScenarioImpl) ScenarioUtils.createScenario( config );
		ScenarioUtils.loadScenario(scenario);
		setControlerSettings(scenario, args);
		// init Benchmark as default
		benchmark = new Benchmark();
	}
	
	/////////////////////////////////////////////////////
	// MATSim preparation
	/////////////////////////////////////////////////////
	
	/**
	 * prepare MATSim for traffic flow simulation ...
	 */
	@SuppressWarnings("deprecation")
	void runMATSim(){
		log.info("Starting MATSim from Urbansim");	

		// checking if this is a test run
		// a test run only validates the xml config file by initializing the xml config via the xsd.
		isTestRun();

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		Network network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);
		
		// get the data from UrbanSim (parcels and persons)
		// readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(), null, 0. );
		readFromUrbanSim();
		
		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl parcels = null; // new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		
		// initializing parcels and zones from UrbanSim input
		//readUrbansimParcelModel(parcels, zones);
		if(isParcelMode){
			parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
			// initializing parcels and zones from UrbanSim input
			readFromUrbansim.readFacilitiesParcel(parcels, zones);
		}
		else
			// initializing zones only from UrbanSim input
			readFromUrbansim.readFacilitiesZones(zones);
		
		// population generation
		int pc = benchmark.addMeasure("Population construction");
		Population newPopulation = readUrbansimPersons(parcels, zones, network);
		modifyPopulation(newPopulation);
		benchmark.stoppMeasurement(pc);
		System.out.println("Population construction took: " + benchmark.getDurationInSeconds( pc ) + " seconds.");

		log.info("### DONE with demand generation from urbansim ###");

		// set population in scenario
		scenario.setPopulation(newPopulation);

		// running mobsim and assigned controller listener
		runControler(zones, parcels);
	}
	
	/**
	 * 
	 */
	protected void readFromUrbanSim() {
		// get the data from UrbanSim (parcels and persons)
		if(getUrbanSimParameterConfig().isUseShapefileLocationDistribution()){
			readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(),
					  getUrbanSimParameterConfig().getUrbanSimZoneShapefileLocationDistribution(),
					  getUrbanSimParameterConfig().getUrbanSimZoneRadiusLocationDistribution());
		}
		else
			readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(),
					  null,
					  getUrbanSimParameterConfig().getUrbanSimZoneRadiusLocationDistribution());
	}
	
	/**
	 * read person table from urbansim and build MATSim population
	 * 
	 * @param readFromUrbansim
	 * @param parcelsOrZones
	 * @param network
	 * @return
	 */
	Population readUrbansimPersons(ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl zones, Network network){
		// read UrbanSim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;
		
		MATSim4UrbanSimControlerConfigModuleV3 m4uModule = getMATSim4UrbaSimControlerConfig();
		UrbanSimParameterConfigModuleV3 uspModule		 = getUrbanSimParameterConfig();
		
		
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
			log.info("(MATSim is running in COLD start mode, i.e. MATSim generates new plans-file from UrbanSim input.)" );
			oldPopulation = null;
		}

		// read UrbanSim persons. Generates hwh acts as side effect
		// Population newPopulation = readUrbanSimPopulation(oldPopulation, parcelsOrZones, network, uspModule.getPopulationSampleRate());
		// read UrbanSim persons. Generates hwh acts as side effect
		Population newPopulation;
		if(isParcelMode)
			newPopulation = readFromUrbansim.readPersonsParcel( oldPopulation, parcels, network, uspModule.getPopulationSampleRate() );
		else
			newPopulation = readFromUrbansim.readPersonsZone( oldPopulation, zones, network, uspModule.getPopulationSampleRate() );
		
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
        if (ConfigUtils.addOrGetModule(scenario.getConfig(), RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).isUsingRoadpricing()) {
			controler.addControlerListener(new RoadPricing());
			// yyyy this is a quick fix in order to make the SustainCity case studies work.  The more longterm goal is to
			// remove those "configuration" flags completely from the config.  However, then some other mechanism needs to be found 
			// to be able to configure externally written "scripts" (such as this one) in a simple way.  kai & michael z, feb'13
		}
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(true);	// sets, whether output Graphs are created
		
		log.info("Adding controler listener ...");
		addControlerListener(zones, parcels, controler);
		addFurtherControlerListener(controler, parcels);
		log.info("Adding controler listener done!");
	
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
																					  parcels,
																					  benchmark) );
		if(computeAgentPerformance)
			// creates a persons.csv output for UrbanSim
			controler.addControlerListener(new AgentPerformanceControlerListener(benchmark));
		
		if(computeZoneBasedAccessibilities){
			
			ZoneLayer<Id>  measuringPoints = GridUtils.convertActivityFacilities2ZoneLayer(zones);
			
			ActivityFacilitiesImpl zonesOrParcels;
			if(this.isParcelMode)
				zonesOrParcels = parcels;
			else
				zonesOrParcels = zones;

			// creates zone based table of log sums
			controler.addControlerListener( new ZoneBasedAccessibilityControlerListenerV3( this,
																						measuringPoints, 				
																						zonesOrParcels,
																						benchmark,
																						this.scenario));
		}
		
		if(computeParcelBasedAccessibility){
			SpatialGrid freeSpeedGrid;				// matrix for free speed car related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid carGrid;					// matrix for congested car related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid bikeGrid;					// matrix for bike related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid walkGrid;					// matrix for walk related accessibility measure. based on the boundary (above) and grid size
			
			ZoneLayer<Id>  measuringPoints;

			if(computeParcelBasedAccessibilitiesNetwork){
				measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(cellSizeInMeter, 
																			   nwBoundaryBox.getBoundingBox());
				freeSpeedGrid= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				carGrid = new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				bikeGrid = new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				walkGrid= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
			}
			else{
				Geometry boundary = GridUtils.getBoundary(shapeFile);
				measuringPoints   = GridUtils.createGridLayerByGridSizeByShapeFile(cellSizeInMeter, 
																				   boundary);
				freeSpeedGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				carGrid	= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				bikeGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				walkGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
			}
			
			if(isParcelMode)
				controler.addControlerListener(new ParcelBasedAccessibilityControlerListenerV3( this,
																							 measuringPoints, 
																							 parcels,
																							 freeSpeedGrid,
																							 carGrid,
																							 bikeGrid,
																							 walkGrid, 
																							 benchmark, 
																							 this.scenario));
			else
				controler.addControlerListener(new ParcelBasedAccessibilityControlerListenerV3( this,
																							 measuringPoints, 
																							 zones,
																							 freeSpeedGrid,
																							 carGrid,
																							 bikeGrid,
																							 walkGrid, 
																							 benchmark, 
																							 this.scenario));
		}
		
		// From here output writer for debugging purposes only
		
		if(dumpPopulationData){
			if(isParcelMode)
				readFromUrbansim.readAndDumpPersons2CSV(parcels, 
												 	controler.getNetwork());
			else
				readFromUrbansim.readAndDumpPersons2CSV(zones, 
					 								controler.getNetwork());
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
	
	/**
	 * 
	 * @param scenario
	 * @param args
	 */
	void setControlerSettings(ScenarioImpl scenario, String[] args) {

		AccessibilityParameterConfigModule moduleAccessibility = getAccessibilityParameterConfig();
		MATSim4UrbanSimControlerConfigModuleV3 moduleMATSim4UrbanSim = getMATSim4UrbaSimControlerConfig();
		
		this.opportunitySampleRate 		= moduleAccessibility.getAccessibilityDestinationSamplingRate();

		this.computeAgentPerformance	= moduleMATSim4UrbanSim.isAgentPerformance();
		this.computeZone2ZoneImpedance	= moduleMATSim4UrbanSim.isZone2ZoneImpedance();
		this.computeZoneBasedAccessibilities = moduleMATSim4UrbanSim.isZoneBasedAccessibility();
		this.computeParcelBasedAccessibility	= moduleMATSim4UrbanSim.isCellBasedAccessibility();
		this.computeParcelBasedAccessibilitiesShapeFile = moduleMATSim4UrbanSim.isCellBasedAccessibilityShapeFile();
		this.computeParcelBasedAccessibilitiesNetwork = moduleMATSim4UrbanSim.isCellBasedAccessibilityNetwork();
		this.dumpPopulationData 		= false;
		this.dumpAggegatedWorkplaceData = true;
		
		this.cellSizeInMeter 			= moduleMATSim4UrbanSim.getCellSizeCellBasedAccessibility();
		this.shapeFile					= moduleMATSim4UrbanSim.getShapeFileCellBasedAccessibility();
		// using custom bounding box, defining the study area for accessibility computation
		this.nwBoundaryBox 				= new NetworkBoundaryBox();
		if(Paths.pathExsits(this.shapeFile))						// using shape file for accessibility computation
			log.info("Using shape file for accessibility computation.");		
		else if(moduleMATSim4UrbanSim.isUseCustomBoundingBox()){	// using custom boundary box for accessibility computation
			log.info("Using custon boundig box for accessibility computation.");
			nwBoundaryBox.setCustomBoundaryBox(moduleMATSim4UrbanSim.getBoundingBoxLeft(), 
													moduleMATSim4UrbanSim.getBoundingBoxBottom(), 
													moduleMATSim4UrbanSim.getBoundingBoxRight(), 
													moduleMATSim4UrbanSim.getBoundingBoxTop());
		}
		else{														// using boundary of hole network for accessibility computation
			log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
			nwBoundaryBox.setDefaultBoundaryBox(scenario.getNetwork());
		}
		
		this.timeOfDay					= moduleMATSim4UrbanSim.getTimeOfDay();
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
	void matsim4UrbanSimShutdown(){
		BackupMATSimOutput.prepareHotStart(scenario);
		BackupMATSimOutput.runBackup(scenario);
	}
	
	/**
	 * 
	 */
	void isTestRun(){
		if(getUrbanSimParameterConfig().isTestRun()){
			log.info("TestRun was successful...");
			MATSim4UrbanSimParcel.isSuccessfulMATSimRun = true;
			return;
		}
	}
	
	/**
	 * access to AccessibilityParameterConfigModule and related parameter settings
	 * @return AccessibilityParameterConfigModule
	 */
	AccessibilityParameterConfigModule getAccessibilityParameterConfig() {
		Module m = this.scenario.getConfig().getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
		this.scenario.getConfig().getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
		return apcm;
	}
	
	/**
	 * access to MATSim4UrbanSimControlerConfigModuleV3 and related parameter settings
	 * @return MATSim4UrbanSimControlerConfigModuleV3
	 */
	MATSim4UrbanSimControlerConfigModuleV3 getMATSim4UrbaSimControlerConfig() {
		Module m = this.scenario.getConfig().getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModuleV3) {
			return (MATSim4UrbanSimControlerConfigModuleV3) m;
		}
		MATSim4UrbanSimControlerConfigModuleV3 mccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		this.scenario.getConfig().getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		return mccm;
	}
	
	/**
	 * access to UrbanSimParameterConfigModuleV3 and related parameter settings
	 * @return UrbanSimParameterConfigModuleV3
	 */
	UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfig() {
		Module m = this.scenario.getConfig().getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		this.scenario.getConfig().getModules().put(UrbanSimParameterConfigModuleV3.GROUP_NAME, upcm);
		return upcm;
	}
	
	/**
	 * Entry point
	 * @param args UrbanSim command prompt
	 */
	public static void main(String args[]){
		
		long start = System.currentTimeMillis();
		
		MATSim4UrbanSimParcel m4u = new MATSim4UrbanSimParcel(args);
		m4u.runMATSim();
		m4u.matsim4UrbanSimShutdown();
		MATSim4UrbanSimParcel.isSuccessfulMATSimRun = Boolean.TRUE;
		
		log.info("Computation took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}
	
	/**
	 * this method is only called/needed by "matsim4opus.matsim.MATSim4UrbanSimTest"
	 * @return true if run was successful
	 */
	public static boolean getRunStatus(){
		return MATSim4UrbanSimParcel.isSuccessfulMATSimRun;
	}
	
	public ReadFromUrbanSimModel getReadFromUrbanSimModel(){
		return this.readFromUrbansim;
	}
	
	public boolean isParcelMode(){
		return this.isParcelMode;
	}
	
	public double getOpportunitySampleRate(){
		return this.opportunitySampleRate;
	}
}
