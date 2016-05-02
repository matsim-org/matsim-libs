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
package org.matsim.contrib.matsim4urbansim.run;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.GridBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.ZoneBasedAccessibilityControlerListenerV3;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtModule;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigUtils;
import org.matsim.contrib.matsim4urbansim.config.M4UConfigurationConverterV4;
import org.matsim.contrib.matsim4urbansim.config.Matsim4UrbansimConfigGroup;
import org.matsim.contrib.matsim4urbansim.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.AgentPerformanceControlerListener;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.Zone2ZoneImpedancesControlerListener;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.Benchmark;
import org.matsim.contrib.matsim4urbansim.utils.io.BackupMATSimOutput;
import org.matsim.contrib.matsim4urbansim.utils.io.Paths;
import org.matsim.contrib.matsim4urbansim.utils.io.ReadFromUrbanSimModel;
import org.matsim.contrib.matsim4urbansim.utils.io.writer.UrbanSimParcelCSVWriterListener;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.*;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;


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
class MATSim4UrbanSimParcel{

	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimParcel.class);

	// MATSim scenario
	Scenario scenario = null;
	// MATSim4UrbanSim configuration converter
	M4UConfigurationConverterV4 connector = null;
	// Reads UrbanSim Parcel output files
	ReadFromUrbanSimModel readFromUrbansim = null;
	// Benchmarking computation times and hard disc space ... 
	Benchmark benchmark = null;
	// indicates if MATSim run was successful
	static boolean isSuccessfulMATSimRun 			 = false;
	// needed for controler listeners
	AggregationObject[] aggregatedOpportunities = null;

	boolean isParcelMode = true;

	// run selected controler
	boolean computeGridBasedAccessibility			 = false;	// determines whether grid based accessibilities should be calculated
	boolean computeGridBasedAccessibilitiesUsingShapeFile= false;// determines whether to use a shape file boundary defining the area for grid based accessibilities 
	boolean computeGridBasedAccessibilityUsingBoundingBox = false; // determines whether to use a customized bounding box
	boolean computeZoneBasedAccessibilities			 = false;	// determines whether zone based accessibilities should be calculated
	boolean computeZone2ZoneImpedance		   		 = false;	// determines whether zone o zone impedances should be calculated
	boolean computeAgentPerformance					 = false;	// determines whether agent performances should be calculated
	String shapeFile 						 		 = null;
	double cellSizeInMeter 							 = -1;
	BoundingBox nwBoundaryBox				 = null;

	/**
	 * constructor
	 * 
	 * @param args contains at least a reference to 
	 * 		  MATSim4UrbanSim configuration generated by UrbanSim
	 */
	MATSim4UrbanSimParcel(String args[]){

		OutputDirectoryLogging.catchLogEntries();		
		// (collect log messages internally before they can be written to file.  Can be called multiple times without harm.)

		Gbl.printBuildInfo("matsim4urbansim", "/org/matsim/contrib/matsim4urbansim/revision.txt");

		// Stores location of MATSim configuration file
		String matsimConfiFile = (args!= null && args.length==1) ? args[0].trim():null;
		// checks if args parameter contains a valid path
		Paths.isValidPath(matsimConfiFile);

		connector = new M4UConfigurationConverterV4( matsimConfiFile );
		if( !(connector.init()) ){
			log.error("An error occured while initializing MATSim scenario ...");
			throw new RuntimeException("An error occured while initializing MATSim scenario ...") ;
		}

		scenario = ScenarioUtils.loadScenario( connector.getConfig() );
		setControlerSettings();
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

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		Network network = scenario.getNetwork();
		modifyNetwork(network);
		cleanNetwork(network);

		// get the data from UrbanSim (parcels and persons)
		prepareReadFromUrbanSim();

		// read UrbanSim facilities (these are simply those entities that have the coordinates!)
//		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim parcels") ;
		ActivityFacilitiesImpl parcels = (ActivityFacilitiesImpl) scenario.getActivityFacilities() ;

		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		ActivityFacilitiesImpl opportunities = new ActivityFacilitiesImpl("opportunity locations (e.g. workplaces) for zones or parcels");
		// yyyy parcels and opportunities should be come one ...
		// yyyy ... and then become the matsim activity facilities.

		// initializing parcels and zones from UrbanSim input
		if(isParcelMode){
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
		((MutableScenario) scenario).setPopulation(newPopulation);

		// running mobsim and assigned controller listener
		runControler(zones, parcels, opportunities);
	}

	private void prepareReadFromUrbanSim() {
		// get the data from UrbanSim (parcels and persons)
		if(getMATSim4UrbanSimControlerConfig().usingShapefileLocationDistribution()){
			readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(),
					getMATSim4UrbanSimControlerConfig().getUrbansimZoneRandomLocationDistributionShapeFile(),
					getMATSim4UrbanSimControlerConfig().getUrbanSimZoneRadiusLocationDistribution(), 
					this.scenario.getConfig());
		}
		else{
			readFromUrbansim = new ReadFromUrbanSimModel( getUrbanSimParameterConfig().getYear(),
					null,
					getMATSim4UrbanSimControlerConfig().getUrbanSimZoneRadiusLocationDistribution(), 
					this.scenario.getConfig());
		}
	}

	/**
	 * read person table from urbansim and build MATSim population
	 * 
	 * @param network
	 * @return
	 */
	Population readUrbansimPersons(ActivityFacilitiesImpl parcels, ActivityFacilitiesImpl zones, Network network){
		// read UrbanSim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;

		UrbanSimParameterConfigModuleV3 uspModule = getUrbanSimParameterConfig();

		// check for existing plans file
		if ( scenario.getConfig().plans().getInputFile() != null ) {
			log.info("MATSim is running in WARM/HOT start mode, i.e. MATSim starts with pre-existing pop file:" + scenario.getConfig().plans().getInputFile());
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
		if (ConfigUtils.addOrGetModule(scenario.getConfig(), Matsim4UrbansimConfigGroup.GROUP_NAME, Matsim4UrbansimConfigGroup.class).isUsingRoadPricing()) {
			controler.setModules(new ControlerDefaultsWithRoadPricingModule());
			//  this is a quick fix in order to make the SustainCity case studies work.  The more longterm goal is to
			// remove those "configuration" flags completely from the config.  However, then some other mechanism needs to be found 
			// to be able to configure externally written "scripts" (such as this one) in a simple way.  kai & michael z, feb'13

			// this is now no longer a hack but something that should be reasonably stable.  NOTE: You have to switch on/off roadpricing now in a 
			// (newly constructed) matsim4Urbansim config section.  kai & michael z, sep'14
		}
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles ) ;
		controler.getConfig().controler().setCreateGraphs(true);

		PtMatrix ptMatrix = null ;
		MatrixBasedPtRouterConfigGroup ippcm = ConfigUtils.addOrGetModule(scenario.getConfig(), MatrixBasedPtRouterConfigGroup.GROUP_NAME, MatrixBasedPtRouterConfigGroup.class) ;
		if(ippcm.getPtStopsInputFile() != null){
			log.info("Initializing MATSim4UrbanSim pseudo pt router ...");
			BoundingBox nbb = BoundingBox.createBoundingBox(controler.getScenario().getNetwork());
			ptMatrix = PtMatrix.createPtMatrix(controler.getScenario().getConfig().plansCalcRoute(), nbb, ConfigUtils.addOrGetModule(controler.getScenario().getConfig(), MatrixBasedPtRouterConfigGroup.GROUP_NAME, MatrixBasedPtRouterConfigGroup.class));	
			controler.addOverridingModule(new MatrixBasedPtModule()); // the car and pt router

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
	final void addControlerListener(final ActivityFacilitiesImpl zones, final ActivityFacilitiesImpl parcels, final ActivityFacilitiesImpl opportunities, final Controler controler, final PtMatrix ptMatrix) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// The following lines register what should be done _after_ the iterations are done:
				if(computeZone2ZoneImpedance || MATSim4UrbanSimZone.BRUSSELS_SCENARIO_CALCULATE_ZONE2ZONE_MATRIX) {
					// (for the time being, this is computed for the Brussels scenario no matter what, since it is needed for the travel time
					// comparisons. kai, apr'13)

					// creates zone2zone impedance matrix
					addControlerListenerBinding().toInstance( new Zone2ZoneImpedancesControlerListener( zones,
							parcels,
							ptMatrix,
							benchmark) );
				}

				if(computeAgentPerformance) {
					// creates a persons.csv output for UrbanSim
					UrbanSimParameterConfigModuleV3 module = M4UConfigUtils.getUrbanSimParameterConfigAndPossiblyConvert(getConfig());
					addControlerListenerBinding().toInstance(new AgentPerformanceControlerListener(benchmark, ptMatrix, module));
				}

				if(computeZoneBasedAccessibilities){
					// creates zone based table of log sums
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
						@Override
						public ControlerListener get() {
							ZoneBasedAccessibilityControlerListenerV3 zbacl = new ZoneBasedAccessibilityControlerListenerV3( zones,
									opportunities,
									ptMatrix,
									((UrbanSimParameterConfigModuleV3) getConfig().getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME)).getMATSim4OpusTemp(),
									scenario, travelTimes, travelDisutilityFactories);
							for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
								zbacl.setComputingAccessibilityForMode(mode, true);
							}
							if ( ptMatrix==null ) {
								zbacl.setComputingAccessibilityForMode(Modes4Accessibility.pt, false);
								// somewhat stupid fix. kai, jan'2015
							}
							return zbacl;
						}
					});

					log.error("yyyy I think that ZoneBasedAccessibilityControlerListener and GridBasedAccessibilityControlerListener are writing " +
							"to the same file!!!!  Check, and fix if true.  kai, jul'13") ;

				}

				if(computeGridBasedAccessibility){
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
						@Inject Scenario scenario;
						@Override
						public ControlerListener get() {
							// initializing grid based accessibility controler listener
							GridBasedAccessibilityControlerListenerV3 gbacl =
									new GridBasedAccessibilityControlerListenerV3( opportunities, ptMatrix, scenario.getConfig(), scenario, travelTimes, travelDisutilityFactories);
							gbacl.setUrbansimMode(true);

							for ( Modes4Accessibility mode : Modes4Accessibility.values() ) {
								gbacl.setComputingAccessibilityForMode(mode, true);
							}
							if ( ptMatrix==null ) {
								gbacl.setComputingAccessibilityForMode( Modes4Accessibility.pt, false );
								// somewhat stupid fix. kai, jan'2015
							}

							if(computeGridBasedAccessibilitiesUsingShapeFile) {
								gbacl.generateGridsAndMeasuringPointsByShapeFile(shapeFile, cellSizeInMeter);
							} else if(computeGridBasedAccessibilityUsingBoundingBox) {
								gbacl.generateGridsAndMeasuringPointsByCustomBoundary(nwBoundaryBox.getXMin(), nwBoundaryBox.getYMin(), nwBoundaryBox.getXMax(), nwBoundaryBox.getYMax(), cellSizeInMeter);
							} else {
								gbacl.generateGridsAndMeasuringPointsByNetwork(cellSizeInMeter);
							}

							if(isParcelMode){
								// creating a writer listener that writes out accessibility results in UrbanSim format for parcels
								UrbanSimParcelCSVWriterListener csvParcelWiterListener = new UrbanSimParcelCSVWriterListener(parcels,scenario.getConfig());
								// the writer listener is added to grid based accessibility controler listener and will be triggered when accessibility calculations are done.
								// (adding such a listener is optional, here its done to be compatible with UrbanSim)
								gbacl.addSpatialGridDataExchangeListener(csvParcelWiterListener);
							}

							// accessibility calculations will be triggered when mobsim finished
							return gbacl;
						}
					});


				}

				// From here outputs are for analysis/debugging purposes only
				{ // dump population in csv format
					if(isParcelMode)
						readFromUrbansim.readAndDumpPersons2CSV(parcels, scenario.getNetwork());
					//		else
					//			readFromUrbansim.readAndDumpPersons2CSV(zones, controler.getNetwork());
					// I don't think that this has a chance to work: uses the parcel model keys to extract info from a zone
					// model person file.  kai, jul'13
				}

			}
		});
	}

	/**
	 * This method allows to add additional listener
	 * This needs to be implemented by another class
	 * @param zones
	 * @param parcels 
	 * @param controler 
	 */
	void addFurtherControlerListener(ActivityFacilities zones, ActivityFacilities parcels, MatsimServices controler){
		// this is just a stub and does nothing. 
		// This needs to be implemented/overwritten by an inherited class
	}

	/**
	 * Getting parameter  
	 */
	void setControlerSettings() {

		AccessibilityConfigGroup moduleAccessibility = getAccessibilityParameterConfig();
		UrbanSimParameterConfigModuleV3 moduleUrbanSim = getUrbanSimParameterConfig();

		this.computeAgentPerformance	= moduleUrbanSim.usingAgentPerformance();
		this.computeZone2ZoneImpedance	= moduleUrbanSim.usingZone2ZoneImpedance();
		this.computeZoneBasedAccessibilities = moduleUrbanSim.usingZoneBasedAccessibility();
		this.computeGridBasedAccessibility	= moduleUrbanSim.usingGridBasedAccessibility();

		if ( moduleAccessibility.getAreaOfAccessibilityComputation().equals( AreaOfAccesssibilityComputation.fromBoundingBox.toString() ) ) {
			this.computeGridBasedAccessibilityUsingBoundingBox = true ;
		} else if ( moduleAccessibility.getAreaOfAccessibilityComputation().equals( AreaOfAccesssibilityComputation.fromShapeFile.toString() ) ) {
			this.computeGridBasedAccessibilitiesUsingShapeFile = true ;
		} 
		// if the other two are false, then network is used.


		// this.computeGridBasedAccessibilitiesUsingNetworkBoundary = moduleAccessibility.isCellBasedAccessibilityNetwork();
		this.cellSizeInMeter 			= moduleAccessibility.getCellSizeCellBasedAccessibility();
		this.shapeFile					= moduleAccessibility.getShapeFileCellBasedAccessibility();

		// the boundary box defines the study area for accessibility calculations if no shape file is provided or a zone based UrbanSim application is used
		// the boundary is either defined by a user defined boundary box or if not applicable by the extend of the road network
		if(this.computeGridBasedAccessibilityUsingBoundingBox){	// check if a boundary box is defined
			// log.info("Using custom bounding box for accessibility computation.");
			nwBoundaryBox = BoundingBox.createBoundingBox(moduleAccessibility.getBoundingBoxLeft(), 
					moduleAccessibility.getBoundingBoxBottom(), 
					moduleAccessibility.getBoundingBoxRight(), 
					moduleAccessibility.getBoundingBoxTop());
		}
		else{	// no boundary box defined using boundary of hole network for accessibility computation
			// log.warn("Using the boundary of the network file for accessibility computation. This could lead to memory issues when the network is large and/or the cell size is too fine.");
			nwBoundaryBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		}
	}

	/**
	 * cleaning matsim network
	 * @param network
	 */
	static void cleanNetwork(Network network){
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
	 * access to AccessibilityParameterConfigModule and related parameter settings
	 * @return AccessibilityParameterConfigModule
	 */
	AccessibilityConfigGroup getAccessibilityParameterConfig() {
		ConfigGroup m = this.scenario.getConfig().getModule(AccessibilityConfigGroup.GROUP_NAME);
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
		ConfigGroup m = this.scenario.getConfig().getModule(M4UControlerConfigModuleV3.GROUP_NAME);
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
		ConfigGroup m = this.scenario.getConfig().getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
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
}
