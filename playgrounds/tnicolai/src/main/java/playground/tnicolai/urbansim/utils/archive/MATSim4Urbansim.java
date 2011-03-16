///* *********************************************************************** *
// * project: org.matsim.*
// * MATSim4Urbansim.java
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
//package playground.tnicolai.urbansim.utils.archive;
//
//
//import java.io.File;
//import java.io.IOException;
//
//import javax.xml.XMLConstants;
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.JAXBElement;
//import javax.xml.bind.JAXBException;
//import javax.xml.bind.Unmarshaller;
//import javax.xml.validation.Schema;
//import javax.xml.validation.SchemaFactory;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.config.Config;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.facilities.ActivityFacilitiesImpl;
//import org.matsim.core.facilities.FacilitiesWriter;
//import org.matsim.core.network.NetworkImpl;
//import org.matsim.core.network.algorithms.NetworkCleaner;
//import org.matsim.core.population.PopulationWriter;
//import org.matsim.core.scenario.ScenarioImpl;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.core.utils.misc.ConfigUtils;
//import org.xml.sax.SAXException;
//
//import playground.tnicolai.urbansim.com.matsim.config.MatsimConfigType;
//import playground.tnicolai.urbansim.constants.Constants;
//import playground.tnicolai.urbansim.utils.CommonUtilities;
//import playground.tnicolai.urbansim.utils.MATSimConfigObject;
//import playground.tnicolai.urbansim.utils.MyControlerListener;
//import playground.tnicolai.urbansim.utils.io.LoadFile;
//import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
//
///**
// * @author thomas
// *
// */
//public class MATSim4Urbansim {
//
//	// logger
//	private static final Logger log = Logger.getLogger(MATSim4Urbansim.class);
//
//	// Stores location of MATSim configuration file
//	private static String matsimConfigFile = null;
//	// MATSim configuration
//	private static Config config	= null;
//	// MATSim scenario
//	private static ScenarioImpl scenario = null;
//
//	/**
//	 * Entry point
//	 * @param args urbansim command prompt
//	 */
//	public static int main(String args[]){
//
//		log.info("Starting MATSim from Urbansim");
//
//		// checks if args parameter contains a valid path
//		isValidPath(args);
//		// loading and initializing MATSim config
//		prepareRun();
//		// checking for if this is only a test run
//		// a test run only validates the xml config file by initializing the xml config via the xsd.
//		if(MATSimConfigObject.isTestRun()){
//			log.info("TestRun was successful...");
//			return Constants.TEST_RUN_SUCCESSFUL;
//		}
//
//		// init scenario and config object
//		scenario = MATSimConfigObject.getScenario();
//		config = MATSimConfigObject.getConfig();
//
//		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
//		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
//		NetworkImpl network = scenario.getNetwork();
//
//		log.info("") ;
//		log.info("Cleaning network ...");
//		( new NetworkCleaner() ).run(network);
//		log.info("... finished cleaning network.") ;
//		log.info("") ;
//
//		// get the data from urbansim (parcels and persons)
//		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( MATSimConfigObject.getYear() );
//
//		// read urbansim facilities (these are simply those entities that have the coordinates!)
//		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
//		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
//		readFromUrbansim.readFacilities(facilities, zones);
//
//		// write the facilities from the urbansim parcel model as a compressed locations.xml file into the temporary directory as input for ???
//		new FacilitiesWriter(facilities).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "locations.xml.gz" );
//
//		// read urbansim population (these are simply those entities that have the person, home and work ID)
//		Population oldPopulation = null;
//		if ( config.plans().getInputFile() != null ) {
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
//		readFromUrbansim.readPersons( oldPopulation, newPopulation, facilities, network, MATSimConfigObject.getSampeRate() ) ;
//		oldPopulation=null ;
//		System.gc() ;
//
//		new PopulationWriter(newPopulation,network).write( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "pop.xml.gz" );
//
//		log.info("### DONE with demand generation from urbansim ###") ;
//
//		// set population in scenario
//		scenario.setPopulation(newPopulation);
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
//		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
//		
//		// The following lines register what should be done _after_ the iterations were run:
//		MyControlerListener myControlerListener = new MyControlerListener( zones, null ) ;
//		controler.addControlerListener(myControlerListener);
//
//		// run the iterations, including the post-processing:
//		controler.run() ;
//		
//		return 0;
//	}
//	
//	/**
//	 * verifying if args argument contains a vaild path. 
//	 * @param args
//	 */
//	private static void isValidPath(String args[]){
//		// test the path to matsim config xml
//		if( args==null || args[0].length() <= 0 || !pathExsits( args[0] ) ){
//			log.error(args[0] + " is not a valid path. SHUTDOWN MATSim!");
//			System.exit(Constants.NOT_VALID_PATH);
//		}
//	}
//	
//	/**
//	 * loading, validating and initializing MATSim config.
//	 */
//	private static void prepareRun(){
//		// binding the parameter from the MATSim Config into the JaxB data structure
//		if(!unmaschalMATSimConfig()){ // TODO Set Output Dir for MATSim files???
//			if(MATSimConfigObject.isTestRun()){
//				log.error("TestRun failed !!!");
//				System.exit(Constants.TEST_RUN_FAILD);
//			}
//			else{
//				log.error("Unmarschalling failed. SHUTDOWN MATSim!");
//				System.exit(Constants.UNMARSCHALLING_FAILED);
//			}
//		}
//	}
//	
//	/**
//	 * unmarschal (read) matsim config
//	 * @return
//	 */
//	private static boolean unmaschalMATSimConfig(){
//
//		log.info("Staring unmaschalling MATSim configuration from: " + MATSim4Urbansim.matsimConfigFile );
//		log.info("...");
//		try{
//			JAXBContext jaxbContext = JAXBContext.newInstance(playground.tnicolai.urbansim.com.matsim.config.ObjectFactory.class);
//			// create an unmaschaller (write xml file)
//			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();
//
//			// crate a schema factory ...
//			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
//			// ... and initialize it with an xsd (xsd lies in the urbansim project)
//			
//			LoadFile loadFile = new LoadFile(Constants.MATSim_4_UrbanSim_XSD, CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/", "MATSim4UrbanSimConfigSchema.xsd");
//			File file2XSD = loadFile.loadMATSim4UrbanSimXSD();
//			
//			// for debugging
//			// File file2XSD = new File( "/Users/thomas/Development/workspace/urbansim_trunk/opus_matsim/sustain_city/models/pyxb_xml_parser/MATSim4UrbanSimConfigSchema.xsd" ); 
//			if(file2XSD == null || !file2XSD.exists()){
//				
//				log.warn(file2XSD.getCanonicalPath() + " is not available. Loading compensatory xsd instead (this could be an older xsd version and may not work correctly).");
//				log.warn("Compensatory xsd file: " + CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/MATSim4UrbanSimConfigSchema.xsd");
//				
//				file2XSD = new File(CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/MATSim4UrbanSimConfigSchema.xsd");
//				if(!file2XSD.exists()){
//					log.error(file2XSD.getCanonicalPath() + " not found!!!");
//					return false;
//				}
//			}
//			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
//			// create a schema object via the given xsd to validate the MATSim xml config.
//			Schema schema = schemaFactory.newSchema(file2XSD);
//			// set the schema for validation while reading/importing the MATSim xml config.
//			unmarschaller.setSchema(schema);
//			
//			File inputFile = new File( MATSim4Urbansim.matsimConfigFile );
//			if(!inputFile.exists())
//				log.error(inputFile.getCanonicalPath() + " not found!!!");
//			// contains the content of the MATSim config.
//			Object object = unmarschaller.unmarshal(inputFile);
//			
//			// Java representation of the schema file.
//			MatsimConfigType matsimConfig;
//			
//			// The structure of both objects must match.
//			if(object.getClass() == MatsimConfigType.class)
//				matsimConfig = (MatsimConfigType) object;
//			else
//				matsimConfig = (( JAXBElement<MatsimConfigType>) object).getValue();
//			
//			// creatin MASim config object that contains the values from the xml config file.
//			if(matsimConfig != null){
//				log.info("Creating new MATSim config object to store the values from the xml configuration.");
//				MATSimConfigObject.initMATSimConfigObject(matsimConfig);
//			}
//		}
//		catch(JAXBException je){
//			je.printStackTrace();
//			return false;
//		}
//		catch(SAXException se){
//			se.printStackTrace();
//			return false;
//		}
//		catch(IOException ioe){
//			ioe.printStackTrace();
//			return false;
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			return false;
//		}
//		log.info("... finished unmarschallig");
//		return true;
//	}
//
//	/**
//	 * Checks a given path if it exists
//	 * @param arg path
//	 * @return true if the given file exists
//	 */
//	private static boolean pathExsits(String args){
//		matsimConfigFile = args.trim();
//
//		if( (new File(matsimConfigFile)).exists() )
//			return true;
//		return false;
//	}
//}
//
//

