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
package org.matsim.contrib.matsim4opus.matsim4urbansim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.config.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.utils.AggregateObject2NearestNode;
import org.matsim.contrib.improvedPseudoPt.MATSim4UrbanSimRouterFactoryImpl;
import org.matsim.contrib.improvedPseudoPt.PtMatrix;
import org.matsim.contrib.improvedPseudoPt.config.ImprovedPseudoPtConfigGroup;
import org.matsim.contrib.improvedPseudoPt.config.ImprovedPseudoPtConfigUtils;
import org.matsim.contrib.matsim4opus.config.M4UConfigurationConverterV4;
import org.matsim.contrib.matsim4opus.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.interfaces.MATSim4UrbanSimInterface;
import org.matsim.contrib.matsim4opus.utils.helperObjects.Benchmark;
import org.matsim.contrib.matsim4opus.utils.io.BackupMATSimOutput;
import org.matsim.contrib.matsim4opus.utils.io.Paths;
import org.matsim.contrib.matsim4opus.utils.io.ReadFromUrbanSimModel;
import org.matsim.contrib.matsim4opus.utils.io.writer.UrbanSimParcelCSVWriterListener;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricing;

import com.vividsolutions.jts.geom.Geometry;


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
	M4UConfigurationConverterV4 connector = null;
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
	boolean computeGridBasedAccessibility			 = false;	// determines whether grid based accessibilities should be calculated
	boolean computeGridBasedAccessibilitiesUsingShapeFile= false;// determines whether to use a shape file boundary defining the area for grid based accessibilities 
	boolean computeGridBasedAccessibilityUsingBoundingBox = false; // determines whether to use a customized bounding box
	// boolean computeGridBasedAccessibilitiesUsingNetworkBoundary = false; // may lead to "out of memory" error when either one/some of this is true: high resolution, huge network, less memory
	boolean computeZoneBasedAccessibilities			 = false;	// determines whether zone based accessibilities should be calculated
	boolean computeZone2ZoneImpedance		   		 = false;	// determines whether zone o zone impedances should be calculated
	boolean computeAgentPerformance					 = false;	// determines whether agent performances should be calculated
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

		OutputDirectoryLogging.catchLogEntries();		
		// (collect log messages internally before they can be written to file.  Can be called multiple times without harm.)

		Gbl.printBuildInfo("matsim4urbansim", "/org/matsim/contrib/matsim4opus/revision.txt");
		// yy can't say where the ``component'' name comes from; certainly does nowhere exist in this capitalization (if anything, it is
		// OPUS). kai, may'13
		// from the top-level package name. can be replaced with anything, it's just there to better
		// format the output. mrieser, may'13
		// For the time being, the maven package shows up as matsim4urbansim, although the contrib itself is under 
		// org.matsim.contrib.matsim4opus.  Presumably TN's design decision (?), for unknown reasons.  kai, may'13  
		
		// Stores location of MATSim configuration file
		String matsimConfiFile = (args!= null && args.length==1) ? args[0].trim():null;
		// checks if args parameter contains a valid path
		Paths.isValidPath(matsimConfiFile);
		
		connector = new M4UConfigurationConverterV4( matsimConfiFile );
		if( !(connector.init()) ){
			log.error("An error occured while initializing MATSim scenario ...");
			System.exit(-1);
		}
		
		scenario = (ScenarioImpl) ScenarioUtils.createScenario( connector.getConfig() );
		ScenarioUtils.loadScenario(scenario);
		setControlerSettings(args);
		// init Benchmark as default
		benchmark = new Benchmark();
	}
	
	/////////////////////////////////////////////////////
	// MATSim preparation
	/////////////////////////////////////////////////////
	
	/**
	 * prepare MATSim for iterations ...
	 */
//	@SuppressWarnings("deprecation") // why should these warnings be suppressed? kai, may'12
	void run(){
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
		ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunity locations (e.g. workplaces) for zones or parcels");
		
		// initializing parcels and zones from UrbanSim input
		//readUrbansimParcelModel(parcels, zones);
		if(isParcelMode){
			parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
			// initializing parcels and zones from UrbanSim input
			readFromUrbansim.readFacilitiesParcel(parcels, zones);
			// initializing opportunity facilities (like work places) on parcel level
			readFromUrbansim.readJobs(opportunities, parcels, this.isParcelMode);
		}
		else{
			// initializing zones only from UrbanSim input
			readFromUrbansim.readFacilitiesZones(zones);
			// initializing opportunity facilities (like work places) on zone level
			readFromUrbansim.readJobs(opportunities, zones, this.isParcelMode);
		}
		
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
		runControler(zones, parcels, opportunities);
	}
	
	/**
	 * 
	 */
	protected void readFromUrbanSim() {
		// get the data from UrbanSim (parcels and persons)
		if(getUrbanSimParameterConfig().isUsingShapefileLocationDistribution()){
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
		
		M4UControlerConfigModuleV3 m4uModule = getMATSim4UrbanSimControlerConfig();
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
	 */
	void runControler( ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels,ActivityFacilitiesImpl opportunities){
		
		Controler controler = new Controler(scenario);
		if (scenario.getConfig().scenario().isUseRoadpricing()) {
			controler.addControlerListener(new RoadPricing());
			// yyyy this is a quick fix in order to make the SustainCity case studies work.  The more longterm goal is to
			// remove those "configuration" flags completely from the config.  However, then some other mechanism needs to be found 
			// to be able to configure externally written "scripts" (such as this one) in a simple way.  kai & michael z, feb'13
		}
		controler.setOverwriteFiles(true);	// sets whether output files are overwritten
		controler.setCreateGraphs(true);	// sets whether output graphs are created
		
		PtMatrix ptMatrix = null ;
		ImprovedPseudoPtConfigGroup ippcm = ImprovedPseudoPtConfigUtils.getConfigModuleAndPossiblyConvert(scenario.getConfig()) ;
		if(ippcm.getPtStopsInputFile() != null){
			log.info("Initializing MATSim4UrbanSim pseudo pt router ...");
			// will lead to a null pointer anyway, but since the 
			// routerFactory has changed an initialization of plansCalcRoute
			// as it was done before is no longer possible. Thus, I think a 
			// more meaningful message seems to be helpful. Daniel, May '13
			if(ippcm.getPtTravelDistancesInputFile() == null || ippcm.getPtTravelTimesInputFile() == null){
				if(controler.getScenario().getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.pt) == null){
					throw new RuntimeException("you try to run the pseudo-pt-router without distances and/or traveltimes, " +
							"but without a teleportedModeSpeed for pt as well. Default is teleportedModeFreespeedFactor...");
				}
				if(controler.getScenario().getConfig().plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) == null){
					throw new RuntimeException("you try to run the pseudo-pt-router without distances and/or traveltimes, " +
							"but without a teleportedModeSpeed for walk as well. Default is teleportedModeFreespeed...");
				}
			}
			
			// if ptStops etc are given in config
			PlansCalcRouteConfigGroup plansCalcRoute = controler.getScenario().getConfig().plansCalcRoute();
			// determining the bounds minX/minY -- maxX/maxY. For optimal performance of the QuadTree. All pt stops should be evenly distributed within this rectangle.
			NetworkBoundaryBox nbb = new NetworkBoundaryBox();
			nbb.setDefaultBoundaryBox(controler.getScenario().getNetwork());
			ptMatrix = new PtMatrix(controler.getScenario().getNetwork(),
									plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.walk),
									plansCalcRoute.getTeleportedModeSpeeds().get(TransportMode.pt),
									plansCalcRoute.getBeelineDistanceFactor(),
									nbb.getXMin(), nbb.getYMin(), nbb.getXMax(), nbb.getYMax(),
									ImprovedPseudoPtConfigUtils.getConfigModuleAndPossiblyConvert(controler.getScenario().getConfig()));	
			controler.setTripRouterFactory( new MATSim4UrbanSimRouterFactoryImpl(controler, ptMatrix) ); // the car and pt router
			
			log.error("reconstructing pt route distances; not tested ...") ;
			for ( Person person : scenario.getPopulation().getPersons().values() ) {
				for ( Plan plan : person.getPlans() ) {
					for ( PlanElement pe : plan.getPlanElements() ) {
						if ( pe instanceof Leg ) {
							Leg leg = (Leg) pe ;
							if ( leg.getMode().equals(TransportMode.pt) ) {
								Activity fromAct = ((PlanImpl)plan).getPreviousActivity(leg) ;
								Activity toAct = ((PlanImpl)plan).getNextActivity(leg) ;
								Route route = leg.getRoute() ;
								route.setDistance( ptMatrix.getPtTravelDistance_meter(fromAct.getCoord(), toAct.getCoord()) ) ;
							}
						}
					}
				}
			}
			
		}
			
		log.info("Adding controler listener ...");
		addControlerListener(zones, parcels, opportunities, controler, ptMatrix);
		addFurtherControlerListener(zones, parcels, controler);
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
	final void addControlerListener(ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl opportunities, Controler controler, PtMatrix ptMatrix) {

		// The following lines register what should be done _after_ the iterations are done:
		if(computeZone2ZoneImpedance || MATSim4UrbanSimZone.BRUSSELS_SCENARIO_CALCULATE_ZONE2ZONE_MATRIX) {
			// (for the time being, this is computed for the Brussels scenario no matter what, since it is needed for the travel time 
			// comparisons. kai, apr'13)
			
			// creates zone2zone impedance matrix
			controler.addControlerListener( new Zone2ZoneImpedancesControlerListener( this,
																					  zones, 
																					  parcels,
																					  ptMatrix,
																					  this.benchmark) );
		}
		if(computeAgentPerformance)
			// creates a persons.csv output for UrbanSim
			controler.addControlerListener(new AgentPerformanceControlerListener(benchmark, ptMatrix));
		
		if(computeZoneBasedAccessibilities){
			// creates zone based table of log sums
			controler.addControlerListener( new ZoneBasedAccessibilityControlerListenerV3(zones,
																						opportunities,
																						ptMatrix,
																						InternalConstants.MATSIM_4_OPUS_TEMP,
																						this.scenario));
		}
		
		if(computeGridBasedAccessibility){
			// "measuringPoints" incorporate the coordinates for which the accessibility will be calculated.
			// Accessibilities can be computed for several transport modes such as free speed and congested car,
			// bicycle, walk or public transport. For each mode the same measuring point (or origin) is used. 
			// The measured values are stored separately in containers called SpatialGrid (see below). Those 
			// SpratialGrids are 2d arrays that are consistent with the measuring points, i.e. they have the 
			// same shape and number of bins as the measuring points.
			// The reasons to establish two similar structures measuring points and spatial grids are based on
			// the fact that SpatialGrids are optional and can be provided for the transport modes of interest, 
			// i.e. there is no default structure where to get the measuring points from. Another aspect is that
			// SpatialGrids were originally designed to store values only. It would be possible to provide the 
			// centroids of its array bins as measuring points either by storing this additional information 
			// internally or by computing the centroid by demand.
			ActivityFacilitiesImpl measuringPoints;
			
			// Spatial grids are 2d arrays that are storing the measured accessibility values for the given measuring points. 
			SpatialGrid freeSpeedGrid;				// matrix for free speed car related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid carGrid;					// matrix for congested car related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid bikeGrid;					// matrix for bike related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid walkGrid;					// matrix for walk related accessibility measure. based on the boundary (above) and grid size
			SpatialGrid ptGrid;						// matrix for pt related accessibility measure. based on the boundary (above) and grid size

			// this only applies for (i) UrbanSim parcel application with (ii) a provided shape file 
			// that defines the boundary of the study area (without any subdivisions like zones or fazes)
			if(computeGridBasedAccessibilitiesUsingShapeFile){
				if(Paths.pathExsits(this.shapeFile))	// using shape file for accessibility computation
					log.info("Using shape file to determine the area for accessibility computation.");	
				
				Geometry boundary = GridUtils.getBoundary(shapeFile);

				measuringPoints = GridUtils.createGridLayerByGridSizeByShapeFileV2(cellSizeInMeter, boundary);
				freeSpeedGrid= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				carGrid		= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				bikeGrid	= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				walkGrid	= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
				ptGrid		= GridUtils.createSpatialGridByShapeBoundary(cellSizeInMeter, boundary);
			}
			// this normally applies for UrbanSim zone applications. The reason is the provided shape
			// file including subdivisions like zones. This prevents to determine the boundary of the study area.
			else{
				if(this.computeGridBasedAccessibilityUsingBoundingBox)
					log.info("Using custom bounding box to determine the area for accessibility computation.");
				else
					log.warn("Using the boundary of the network file to determine the area for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine!");
				
				measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(cellSizeInMeter, nwBoundaryBox.getXMin(), nwBoundaryBox.getYMin(), nwBoundaryBox.getXMax(), nwBoundaryBox.getYMax() );
				freeSpeedGrid= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				carGrid 	= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				bikeGrid 	= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				walkGrid	= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
				ptGrid		= new SpatialGrid(nwBoundaryBox.getBoundingBox(), cellSizeInMeter);
			}
			
			if(isParcelMode){
				// initializing grid based accessibility controler listener
				GridBasedAccessibilityControlerListenerV3 gbacl = new GridBasedAccessibilityControlerListenerV3( measuringPoints,
																												 opportunities,
																												 ptMatrix,
																												 scenario.getConfig(), 
																												 scenario.getNetwork() );
				// setting SpatialGrids
				gbacl.setFreeSpeedCarGrid(freeSpeedGrid);
				gbacl.setCarGrid(carGrid);
				gbacl.setBikeGrid(bikeGrid);
				gbacl.setWalkGrid(walkGrid);
				gbacl.setPTGrid(ptGrid);
				// accessibility calculations will be triggered when mobsim finished
				controler.addControlerListener(gbacl);
				// creating a writer listener that writes out accessibility results in UrbanSim format for parcels
				UrbanSimParcelCSVWriterListener csvParcelWiterListener = new UrbanSimParcelCSVWriterListener(parcels);
				// the writer listener is added to grid based accessibility controler listener and will be triggered when accessibility calculations are done.
				// (adding such a listener is optional, here its done to be compatible with UrbanSim)
				gbacl.addSpatialGridDataExchangeListener(csvParcelWiterListener);
			}
			else
				controler.addControlerListener(new GridBasedAccessibilityControlerListenerV3(measuringPoints,
																							 opportunities,
																							 ptMatrix,
																							 scenario.getConfig(), 
																							 scenario.getNetwork() ));
		}
		
		// From here outputs are for analysis/debugging purposes only
		{ // dump population in csv format
		if(isParcelMode)
			readFromUrbansim.readAndDumpPersons2CSV(parcels, controler.getNetwork());
		else
			readFromUrbansim.readAndDumpPersons2CSV(zones, controler.getNetwork());
		}
	}
	
	/**
	 * This method allows to add additional listener
	 * This needs to be implemented by another class
	 * @param zones
	 * @param parcels 
	 * @param controler 
	 */
	void addFurtherControlerListener(ActivityFacilities zones, ActivityFacilities parcels, Controler controler){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by an inherited class
	}
	
	/**
	 * 
	 * @param args
	 */
	void setControlerSettings(String[] args) {

		AccessibilityConfigGroup moduleAccessibility = getAccessibilityParameterConfig();
		M4UControlerConfigModuleV3 moduleMATSim4UrbanSim = getMATSim4UrbanSimControlerConfig();
		
		this.opportunitySampleRate 		= moduleAccessibility.getAccessibilityDestinationSamplingRate();

		this.computeAgentPerformance	= moduleMATSim4UrbanSim.isAgentPerformance();
		this.computeZone2ZoneImpedance	= moduleMATSim4UrbanSim.isZone2ZoneImpedance();
		this.computeZoneBasedAccessibilities = moduleMATSim4UrbanSim.isZoneBasedAccessibility();
		this.computeGridBasedAccessibility	= moduleMATSim4UrbanSim.isCellBasedAccessibility();
		this.computeGridBasedAccessibilitiesUsingShapeFile = moduleAccessibility.isCellBasedAccessibilityShapeFile();
		this.computeGridBasedAccessibilityUsingBoundingBox = moduleAccessibility.usingCustomBoundingBox();
		// this.computeGridBasedAccessibilitiesUsingNetworkBoundary = moduleAccessibility.isCellBasedAccessibilityNetwork();
		this.cellSizeInMeter 			= moduleAccessibility.getCellSizeCellBasedAccessibility();
		this.shapeFile					= moduleAccessibility.getShapeFileCellBasedAccessibility();
		
		// the boundary box defines the study area for accessibility calculations if no shape file is provided or a zone based UrbanSim application is used
		// the boundary is either defined by a user defined boundary box or if not applicable by the extend of the road network
		this.nwBoundaryBox 				= new NetworkBoundaryBox();
		if(this.computeGridBasedAccessibilityUsingBoundingBox){	// check if a boundary box is defined
			// log.info("Using custom bounding box for accessibility computation.");
			nwBoundaryBox.setCustomBoundaryBox(moduleAccessibility.getBoundingBoxLeft(), 
													moduleAccessibility.getBoundingBoxBottom(), 
													moduleAccessibility.getBoundingBoxRight(), 
													moduleAccessibility.getBoundingBoxTop());
		}
		else{	// no boundary box defined using boundary of hole network for accessibility computation
			// log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
			nwBoundaryBox.setDefaultBoundaryBox(scenario.getNetwork());
		}
//		if(Paths.pathExsits(this.shapeFile))						// using shape file for accessibility computation
//			log.info("Using shape file for accessibility computation.");		
//		else if(moduleAccessibility.usingCustomBoundingBox()){	// using custom boundary box for accessibility computation
//			log.info("Using custom bounding box for accessibility computation.");
//			nwBoundaryBox.setCustomBoundaryBox(moduleAccessibility.getBoundingBoxLeft(), 
//													moduleAccessibility.getBoundingBoxBottom(), 
//													moduleAccessibility.getBoundingBoxRight(), 
//													moduleAccessibility.getBoundingBoxTop());
//		}
//		else{														// using boundary of hole network for accessibility computation
//			log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
//			nwBoundaryBox.setDefaultBoundaryBox(scenario.getNetwork());
//		}
		this.timeOfDay					= moduleAccessibility.getTimeOfDay();
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
	AccessibilityConfigGroup getAccessibilityParameterConfig() {
		Module m = this.scenario.getConfig().getModule(AccessibilityConfigGroup.GROUP_NAME);
		if (m instanceof AccessibilityConfigGroup) {
			return (AccessibilityConfigGroup) m;
		}
		AccessibilityConfigGroup apcm = new AccessibilityConfigGroup();
		this.scenario.getConfig().getModules().put(AccessibilityConfigGroup.GROUP_NAME, apcm);
		return apcm;
	}
	
	/**
	 * access to MATSim4UrbanSimControlerConfigModuleV3 and related parameter settings
	 * @return MATSim4UrbanSimControlerConfigModuleV3
	 */
	M4UControlerConfigModuleV3 getMATSim4UrbanSimControlerConfig() {
		Module m = this.scenario.getConfig().getModule(M4UControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof M4UControlerConfigModuleV3) {
			return (M4UControlerConfigModuleV3) m;
		}
		M4UControlerConfigModuleV3 mccm = new M4UControlerConfigModuleV3();
		this.scenario.getConfig().getModules().put(M4UControlerConfigModuleV3.GROUP_NAME, mccm);
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
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3();
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
		m4u.run();
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
	
	public double getTimeOfDay(){
		return this.timeOfDay;
	}
	
}
