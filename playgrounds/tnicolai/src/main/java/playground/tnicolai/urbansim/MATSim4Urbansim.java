/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4Urbansim.java
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
package playground.tnicolai.urbansim;


import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.JAXBUnmaschal;
import playground.tnicolai.urbansim.utils.MATSimConfigObject;
import playground.tnicolai.urbansim.utils.MyControlerListener;
import playground.tnicolai.urbansim.utils.WorkplaceObject;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;

/**
 * @author thomas
 *
 */
public class MATSim4Urbansim {

	// logger
	private static final Logger log = Logger.getLogger(MATSim4Urbansim.class);

	// Stores location of MATSim configuration file
	private String matsimConfigFile = null;
	// MATSim configuration
	protected Config config	= null;
	// MATSim scenario
	protected ScenarioImpl scenario = null;
	
	public MATSim4Urbansim(String args[]){

		// checks if args parameter contains a valid path
		isValidPath(args);
		// loading and initializing MATSim config
		init();
	}
	
	public void runMATSim(){
		log.info("Starting MATSim from Urbansim");

		// checking for if this is only a test run
		// a test run only validates the xml config file by initializing the xml config via the xsd.
		if(MATSimConfigObject.isTestRun()){
			log.info("TestRun was successful...");
			return;
		}

		// init scenario and config object
		scenario = MATSimConfigObject.getScenario();
		config = MATSimConfigObject.getConfig();

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkImpl network = scenario.getNetwork();
		cleanNetwork(network);
		
		// get the data from urbansim (parcels and persons)
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( MATSimConfigObject.getYear() );
		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		
		readUrbansimParcelModel(readFromUrbansim, facilities, zones);
		Population newPopulation = readUrbansimPersons(readFromUrbansim, facilities, network);
		Map<Id,WorkplaceObject> numberOfWorkplacesPerZone = ReadUrbansimJobs(readFromUrbansim);

		log.info("### DONE with demand generation from urbansim ###") ;

		// set population in scenario
		scenario.setPopulation(newPopulation);
		// scenario.setFacilities(facilities); // tnicolai: suggest to implement method

		runControler(zones, numberOfWorkplacesPerZone, facilities);
	}
	
	/**
	 * read urbansim parcel table and build facilities and zones in MATSim
	 * 
	 * @param readFromUrbansim
	 * @param facilities
	 * @param zones
	 */
	protected void readUrbansimParcelModel(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl facilities, ActivityFacilitiesImpl zones){

		readFromUrbansim.readFacilities(facilities, zones);
		// write the facilities from the urbansim parcel model as a compressed locations.xml file into the temporary directory as input for ???
//		new FacilitiesWriter(facilities).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "locations.xml.gz" );
	}
	
	/**
	 * read person table from urbansim and build MATSim population
	 * 
	 * @param readFromUrbansim
	 * @param facilities
	 * @param network
	 * @return
	 */
	protected Population readUrbansimPersons(ReadFromUrbansimParcelModel readFromUrbansim, ActivityFacilitiesImpl facilities, NetworkImpl network){
		// read urbansim population (these are simply those entities that have the person, home and work ID)
		Population oldPopulation = null;
		if ( config.plans().getInputFile() != null ) {
			log.info("Population specified in matsim config file; assuming WARM start with pre-existing pop file.");
			log.info("Persons not found in pre-existing pop file are added; persons no longer in urbansim persons file are removed." ) ;
			oldPopulation = scenario.getPopulation() ;
			log.info("Note that the `continuation of iterations' will only work if you set this up via different config files for") ;
			log.info(" every year and know what you are doing.") ;
		}
		else {
			log.warn("No population specified in matsim config file; assuming COLD start.");
			log.info("(I.e. generate new pop from urbansim files.)" );
			oldPopulation = null;
		}

		Population newPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		// read urbansim persons.  Generates hwh acts as side effect
		readFromUrbansim.readPersons( oldPopulation, newPopulation, facilities, network, MATSimConfigObject.getSampeRate() ) ;
		oldPopulation=null;
		System.gc();
		
		// tnicolai : disabled only for debugging reasons. Activate again!!!
		// new PopulationWriter(newPopulation,network).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "pop.xml.gz" );
		
		return newPopulation;
	}
	
	/**
	 * Reads in the job table from urbansim that contains for every "job_id" the corresponded "parcel_id_work" and "zone_id_work"
	 * and returns an HashMap with the number of job for each zone.
	 * 
	 * @return HashMap
	 */
	protected Map<Id,WorkplaceObject> ReadUrbansimJobs(ReadFromUrbansimParcelModel readFromUrbansim){

		return readFromUrbansim.readJobs();
	}
	
	/**
	 * run simulation
	 * @param zones
	 */
	protected void runControler( ActivityFacilitiesImpl zones, Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl facilities ){
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		// The following lines register what should be done _after_ the iterations were run:
		controler.addControlerListener( new MyControlerListener( zones, numberOfWorkplacesPerZone, facilities ) );
		
		// tnicolai todo: count number of cars per h on a link
		// write ControlerListener that implements AfterMobsimListener (notifyAfterMobsim)
		// get VolumeLinkAnalyzer by "event.getControler.getVolume... and run getVolumesForLink. that returns an int array with the number of cars per hour on an specific link 
		// see also http://matsim.org/docs/controler
		
		
		// run the iterations, including the post-processing:
		controler.run() ;
	}
	
	/**
	 * verifying if args argument contains a valid path. 
	 * @param args
	 */
	private void isValidPath(String args[]){
		// test the path to matsim config xml
		if( args==null || args[0].length() <= 0 || !pathExsits( args[0] ) ){
			log.error(args[0] + " is not a valid path. SHUTDOWN MATSim!");
			System.exit(Constants.NOT_VALID_PATH);
		}
	}
	
	/**
	 * loading, validating and initializing MATSim config.
	 */
	private void init(){
		
		JAXBUnmaschal unmarschal = new JAXBUnmaschal( matsimConfigFile);
		
		// binding the parameter from the MATSim Config into the JaxB data structure
		if(!unmarschal.unmaschalMATSimConfig()){
			if(MATSimConfigObject.isTestRun()){
				log.error("TestRun failed !!!");
				System.exit(Constants.TEST_RUN_FAILD);
			}
			else{
				log.error("Unmarschalling failed. SHUTDOWN MATSim!");
				System.exit(Constants.UNMARSCHALLING_FAILED);
			}
		}
	}

	/**
	 * Checks a given path if it exists
	 * @param arg path
	 * @return true if the given file exists
	 */
	private boolean pathExsits(String args){
		matsimConfigFile = args.trim();

		if( (new File(matsimConfigFile)).exists() )
			return true;
		return false;
	}
	
	/**
	 * 
	 * @param network
	 */
	protected void cleanNetwork(NetworkImpl network){
		log.info("") ;
		log.info("Cleaning network ...");
		( new NetworkCleaner() ).run(network);
		log.info("... finished cleaning network.");
		log.info(""); 
	}
	
	/**
	 * Entry point
	 * @param args urbansim command prompt
	 * @return 0 if simulation successful, >0 else
	 */
	public static void main(String args[]){
		MATSim4Urbansim m4u = new MATSim4Urbansim(args);
		m4u.runMATSim();
	}
}



