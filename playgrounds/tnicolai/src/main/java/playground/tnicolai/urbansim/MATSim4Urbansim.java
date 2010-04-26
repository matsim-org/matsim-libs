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
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.xml.sax.SAXException;

import playground.tnicolai.urbansim.com.matsim.config.ConfigType;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.MyControlerListener;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;

/**
 * @author thomas
 *
 */
public class MATSim4Urbansim {

	// logger
	private static final Logger log = Logger.getLogger(MATSim4Urbansim.class);

	// MATSim configuration file
	private static String matsimConfigFile = null;
	// pointer to network file location
	private static String networkFile = null;
	// number of first iteration of this run
	private static int firstIteration = -1;
	// number of last iteration of this run
	private static int lastIteration = -1;
	// year of this run
	private static int year = -1;
	// denotes the sample rate on which MATSim runs. 0.01 means 1%
	private static double samplingRate = -0.01;
	// points to the directory where urbansim and MATSim exchange data
	private static String tempDirectory = null;
	// flag o indicate the fist urbansim run (if always true equals "warm start")
	private static boolean firstRun = true;

	// MATSim configuration
	private static Config config	= null;
	// MATSim scenario
	private static ScenarioImpl scenario = null;

	/**
	 *
	 * @param args urbansim command prompt
	 */
	public static void main(String args[]){

		log.info("Starting MATSim from Urbansim");

		// test the path to matsim config xml
		if( args==null || args[0].length() <= 0 || !isValidConfigPath( args[0] ) ){
			log.error("Missing path to the MATSim configuration file or path isn't valid. SHUTDOWN MATSim!");
			System.exit(Constants.NOT_VALID_PATH);
		}
		// binding the parameter from the MATSim Config into the JaxB data structure
		if(!unmaschalMATSimConfig()){
			log.error("Unmarschalling failed. SHUTDOWN MATSim!");
			System.exit(Constants.UNMARSCHALLING_FAILED);
		}

		// create config object
		createAndInitializeConfigObject();

		// get the network. Always cleaning it seems a good idea since someone may have modified the input files manually in
		// order to implement policy measures.  Get network early so readXXX can check if links still exist.
		NetworkLayer network = scenario.getNetwork() ;

		log.info("") ;
		log.info("Cleaning network ...");
		( new NetworkCleaner() ).run(network);
		log.info("... finished cleaning network.") ;
		log.info("") ;

		// get the data from urbansim (parcels and persons)
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( year );

		// read urbansim facilities (these are simply those entities that have the coordinates!)
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones      = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilities(facilities, zones);

		// write the facilities from the urbansim parcel model as a compressed locations.xml file into the temporary directory as input for ???
		new FacilitiesWriter(facilities).writeFile( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "locations.xml.gz" );

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

		Population newPopulation = new ScenarioImpl().getPopulation();
		// read urbansim persons.  Generates hwh acts as side effect
		readFromUrbansim.readPersons( oldPopulation, newPopulation, facilities, network, samplingRate ) ;
		oldPopulation=null ;
		System.gc() ;

		new PopulationWriter(newPopulation,network).writeFile( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "pop.xml.gz" );

		log.info("### DONE with demand generation from urbansim ###") ;

		// set population in scenario
		scenario.setPopulation(newPopulation);
		Controler controler = new Controler(scenario) ;
		controler.setOverwriteFiles(true) ;

		// The following lines register what should be done _after_ the iterations were run:
		MyControlerListener myControlerListener = new MyControlerListener( zones ) ;
		controler.addControlerListener(myControlerListener);

		// run the iterations, including the post-processing:
		controler.run() ;
	}

	/**
	 * unmarschal (read) matsim config
	 * @return
	 */
	private static boolean unmaschalMATSimConfig(){

		log.info("Staring unmaschalling MATSim configuration from: " + MATSim4Urbansim.matsimConfigFile );
		log.info("...");
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(playground.tnicolai.urbansim.com.matsim.config.ObjectFactory.class);

			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();

			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// ... and initialize it with an xsd
			File file2XSD = new File( Constants.OPUS_MATSIM_DIRECTORY + "/classesTNicolai/xsd/MATSim4UrbanSimTestConfig2.xsd" );
			if(!file2XSD.exists()){
				log.error(file2XSD.getCanonicalPath() + " does not exsist!!!");
				return false;
			}
			log.info("Using folowing xsd schema: " + file2XSD.getCanonicalPath());
			Schema schema = schemaFactory.newSchema(file2XSD);	// set schema for validation

			unmarschaller.setSchema(schema);

			File inputFile = new File( MATSim4Urbansim.matsimConfigFile );
			if(!inputFile.exists())
				log.error(inputFile.getCanonicalPath() + " does not exsist!!!");

			Object object = unmarschaller.unmarshal(inputFile);

			ConfigType matsimConfig;

			if(object.getClass() == ConfigType.class)
				matsimConfig = (ConfigType) object;
			else
				matsimConfig = (( JAXBElement<ConfigType>) object).getValue();

			if(matsimConfig != null){
				networkFile = matsimConfig.getNetwork().getInputFile();
				firstIteration = matsimConfig.getControler().getFirstIteration().intValue();
				lastIteration = matsimConfig.getControler().getLastIteration().intValue();
				samplingRate = matsimConfig.getUrbansimParameter().getSamplingRate();
				year = matsimConfig.getUrbansimParameter().getYear().intValue();
				tempDirectory = matsimConfig.getUrbansimParameter().getTempDirectory();

				log.info("Network: " + matsimConfig.getNetwork().getInputFile());
				log.info("Controler FirstIteration: " + matsimConfig.getControler().getFirstIteration() + " LastIteration: " + matsimConfig.getControler().getLastIteration() );
				log.info("UrbansimParameter SamplingRate: " + matsimConfig.getUrbansimParameter().getSamplingRate() + " Year: " + matsimConfig.getUrbansimParameter().getYear() + " TempDir: " + matsimConfig.getUrbansimParameter().getTempDirectory());
			}
		}
		catch(JAXBException je){
			je.printStackTrace();
			return false;
		}
		catch(SAXException se){
			se.printStackTrace();
			return false;
		}
		catch(IOException ioe){
			ioe.printStackTrace();
			return false;
		}
		log.info("... finished unmarschallig");
		return true;
	}

	/**
	 * creates a MATSim config object with the parameter from the JaxB data structure
	 */
	private static void createAndInitializeConfigObject(){

		scenario = new ScenarioImpl();
		MATSim4Urbansim.config = scenario.getConfig();

		NetworkConfigGroup networkCG = (NetworkConfigGroup) MATSim4Urbansim.config.getModule(NetworkConfigGroup.GROUP_NAME);
		ControlerConfigGroup controlerCG = (ControlerConfigGroup) MATSim4Urbansim.config.getModule(ControlerConfigGroup.GROUP_NAME);

		networkCG.setInputFile( MATSim4Urbansim.networkFile );
		controlerCG.setFirstIteration( MATSim4Urbansim.firstIteration );
		controlerCG.setLastIteration( MATSim4Urbansim.lastIteration);

		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();

		// old version
//		ScenarioLoaderImpl loader = new ScenarioLoaderImpl("/Users/thomas/Development/workspace/OPUS_MATSim_Config_Test/xmls/MATSim4UrbansimOldConfig.xml");
//		loader.loadScenario();
//		ScenarioImpl scenario = loader.getScenario();
//		config = scenario.getConfig();
	}

	/**
	 *
	 * @param arg
	 * @return
	 */
	private static boolean isValidConfigPath(String args){
		matsimConfigFile = args.trim();

		if( (new File(matsimConfigFile)).exists() )
			return true;
		return false;
	}

//	/**
//	 * gets the custom urbansim run parameter from the MATSim config and sets the variable fields
//	 */
//	private static boolean getRunParameterFromConfigfile(){
//
//		if(config == null)
//			return false;
//
//		try{
//			samplingRate = Double.parseDouble( config.findParam(Constants.MATSIM_CONFIG_MODULE_URBANSIM_PARAMETER, Constants.MATSIM_CONFIG_PARAMETER_SAMPLING_RATE) );
//			year = Integer.parseInt( config.findParam(Constants.MATSIM_CONFIG_MODULE_URBANSIM_PARAMETER, Constants.MATSIM_CONFIG_PARAMETER_YEAR) );
//			tempDirectory = config.findParam(Constants.MATSIM_CONFIG_MODULE_URBANSIM_PARAMETER, Constants.MATSIM_CONFIG_PARAMETER_TEMP_DIRECTORY);
//		}
//		catch(NumberFormatException nfe){
//			nfe.printStackTrace();
//			return false;
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			return false;
//		}
//
//		return true;
//	}

//	/**
//	 * gets the program arguments and sets the variable fields
//	 *
//	 * @param args urbansim command prompt
//	 */
//	private static void getProgramArguments(String args[]){
//		// expected input: /Users/thomas/Development/opus_home/opus_matsim/matsim_config/seattle_matsim_0.xml --year=2001 --samplingRate=0.010000
//		StringTokenizer st;
//		String tmp;
//
//		try{
//			log.info("Detected program arguments:");
//			for(int i = 0; i < args.length; i++){
//				st = new StringTokenizer(args[i],"=");
//				tmp = st.nextToken();
//
//				if(tmp.equalsIgnoreCase("--year")){
//					year = Integer.parseInt( st.nextToken() );
//					log.info("Argument " + i + ", year = " + year);
//					continue;
//				}
//				else if(tmp.equalsIgnoreCase("--samplingRate")){
//					samplingRate = Double.parseDouble( st.nextToken() );
//					log.info("Argument " + i + ", samplingRate = " + samplingRate);
//					continue;
//				}
//				else if(tmp.equalsIgnoreCase("--firstRun")){
//					if(st.nextToken().equalsIgnoreCase("FALSE"))
//						firstRun = false;
//					log.info("Argument " + i + ", firstRun = " + firstRun);
//					continue;
//				}
//				else if(tmp.endsWith(".xml")){
//					matsimConfigFile = tmp;
//					log.info("Argument " + i + ", matsimConfigFile = " + matsimConfigFile);
//					continue;
//				}
//				else
//					log.info("Argument " + i + ", " + tmp);
//			}
//
//			if(firstRun){
//				log.info("This ist MATSim RUN : 1");
//				// reset old parameter in the MATSim properties file
//				MATSimConfigurationManager.resetMATSimProperties();
//			}
//			else{
//				log.info("This ist MATSim RUN : " + MATSimConfigurationManager.getMATSimRunCount() );
//				// get path to the generated MATSim config from the properties file
//				// this is needed because urbansim doesn't know about the this config file
//				matsimConfigFile = MATSimConfigurationManager.getPathToGeneratedMATSimConfig();
//			}
//		}
//		catch(NumberFormatException nfe){
//			nfe.printStackTrace();
//			System.exit(Constants.EXCEPTION_OCCURED);
//		}
//		catch(Exception e){
//			e.printStackTrace();
//			System.exit(Constants.EXCEPTION_OCCURED);
//		}
//	}

	/**
	 * getter for sampling rate
	 * @return
	 */
	public static double getSamplingRate(){
		return MATSim4Urbansim.samplingRate;
	}

	/**
	 * getter for year
	 * @return
	 */
	public static int getYear(){
		return MATSim4Urbansim.year;
	}

	/**
	 * getter for temp directory
	 * @return
	 */
	public static String getTempDirectory(){
		return MATSim4Urbansim.tempDirectory;
	}

}

