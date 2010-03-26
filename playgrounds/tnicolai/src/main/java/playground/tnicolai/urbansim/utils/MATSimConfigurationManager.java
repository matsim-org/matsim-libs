/* *********************************************************************** *
 * project: org.matsim.*
 * PreparationForHotStart.java
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

package playground.tnicolai.urbansim.utils;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.properties.MATSimProperties;
import playground.tnicolai.urbansim.utils.io.FileCopy;

public class MATSimConfigurationManager {
	
	// logger
	private static final Logger log = Logger.getLogger(MATSimConfigurationManager.class);
	
	private static String outputPlansFilePath	= null;
	private static String matimConfigFilePath	= null;
	private static int firstIteration;
	private static int lastIteration;
	
	/**
	 * prepares MATSim for the next urbansim run
	 * 
	 * @param config MATSim config object
	 * @return boolean true if process was successful
	 */
	public static boolean repareForNextRun(Config config){
		
		boolean result = false;
		
		// save the new generated output plans file
		saveOutputPlansFile();
		// create a new MATSim config with updated parameters for the next run
		result = generateNewMATSimConfigFile(config);
		// save the path to generated MATSim config file and number of runs in the MATSim properties file
		saveLinkToGeneratedMATSimConfigInPropertiesFile();
		
		return result;
	}
	
	/**
	 * copies the generated plans file to a save place
	 * 
	 * @return boolean true if copying was successful
	 */
	public static boolean saveOutputPlansFile(){
		
		try{
			// source directory for plans file
			File sourceFile = new File(Constants.OPUS_MATSIM_OUTPUT_DIRECTORY + Constants.GENERATED_PLANS_FILE_NAME);
			if(!sourceFile.exists())
				log.error(Constants.OPUS_MATSIM_OUTPUT_DIRECTORY + Constants.GENERATED_PLANS_FILE_NAME + " not found. SHUTDOWN MATSim");
	
			// destination directory for plans file
			File destinationFile = new File(Constants.MATSIM_CONFIG_DIRECTORY + Constants.GENERATED_PLANS_FILE_NAME );
			outputPlansFilePath = destinationFile.getCanonicalPath();
			
			// copy plans file 
			return FileCopy.fileCopy(sourceFile, destinationFile);
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * creates a new MATSim Config file with updated parameters like
	 * the path of the new plans file or the updated first and last iteration
	 * 
	 * @param currentConfig MASim config object	
	 * @return boolean true if creating new MATSim config was successful
	 */
	public static boolean generateNewMATSimConfigFile(Config currentConfig){
		
		if(currentConfig != null){
			// get parameters from current config object for first and last iteration
			firstIteration = Integer.parseInt( currentConfig.getParam( Constants.MATSIM_CONFIG_MODULE_CONTROLLER, Constants.FIRST_ITERATION ) );
			lastIteration = Integer.parseInt( currentConfig.getParam( Constants.MATSIM_CONFIG_MODULE_CONTROLLER, Constants.LAST_ITERATION ) );
			log.info("Current parameter for first iteration in MATSim Config file = " + firstIteration);
			log.info("LastIteration in current MATSim Config file = " + lastIteration);
			// update these parameters for the next run
			int difference = lastIteration - firstIteration;
			if(difference < 0)
				log.warn("Parameter for the first and last itration are not correct!");
			firstIteration = lastIteration + 1;
			lastIteration = firstIteration + difference;
			
			// update parameter in config object
			
			// update first run
			currentConfig.setParam(Constants.MATSIM_CONFIG_MODULE_CONTROLLER, Constants.FIRST_ITERATION, firstIteration+"");
			// update last run
			currentConfig.setParam(Constants.MATSIM_CONFIG_MODULE_CONTROLLER, Constants.LAST_ITERATION, lastIteration+"");
			// update output plans file XML path
			currentConfig.setParam(Constants.MATSIM_CONFIG_MODULE_PLANS, Constants.PLANS_FILE_PATH, outputPlansFilePath);
			
			log.info("New parameter for first iteration in MATSim Config file = " + firstIteration);
			log.info("New parameter for last iteration in MATSim Config file = " + lastIteration);
			log.info("New parameter for plans input in MATSim Config file = " + outputPlansFilePath);
			
			// write new config object on hard disk
			log.info("Start writing new MATSim config file.");
			matimConfigFilePath = Constants.MATSIM_CONFIG_DIRECTORY + Constants.GENERATED_MATSIM_CONFIG_FILE_NAME;
			ConfigWriter configWriter = new ConfigWriter(currentConfig);
			configWriter.writeFile(matimConfigFilePath);
			log.info("Writing new MATSim config file finished.");
			
			return true;
		}
		
		// else, if not successful return false
		log.error("Config object ist null!");
		return false;
	}
	
	/**
	 * save the path to generated MATSim config file and number of runs 
	 * in the MATSim properties file
	 */
	private static void saveLinkToGeneratedMATSimConfigInPropertiesFile(){
		
		try{
			int run = Integer.parseInt( getMATSimRunCount() );
		
			MATSimProperties.properies.setProperty(Constants.RUN_NUMBER, (run + 1)+"");
			MATSimProperties.properies.setProperty(Constants.PATH_TO_GENERATED_MATSIM_CONFIG_FILE, outputPlansFilePath);	
			MATSimProperties.properies.saveMATSimState();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * returns the number of runs from the MATSim properties file
	 * 
	 * @return String number of runs
	 */
	public static String getMATSimRunCount(){
		return MATSimProperties.properies.getProperty(Constants.RUN_NUMBER);
	}
	
	/**
	 * returns the path to the generated MATSim config from the properties file
	 * 
	 * @return String path to generated MATSim config
	 */
	public static String getPathToGeneratedMATSimConfig(){
		return MATSimProperties.properies.getProperty(Constants.PATH_TO_GENERATED_MATSIM_CONFIG_FILE);
	}
	
	/**
	 * set the number of runs to 1 and clears the path to the generated MATSim config file
	 * when its the first run of MATSim
	 */
	public static void resetMATSimProperties(){
		MATSimProperties.properies.setProperty(Constants.RUN_NUMBER, "1");
		MATSimProperties.properies.setProperty(Constants.PATH_TO_GENERATED_MATSIM_CONFIG_FILE, "");
		MATSimProperties.properies.saveMATSimState();
	}

	/**
	 * for testing purposes
	 * @param args
	 */
	public static void main(String args[]){
		
		Scenario scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
		
		MatsimConfigReader reader = new MatsimConfigReader(config);
		reader.readFile(Constants.DEFAULT_MATSIM_CONFIG_FILE);	// inits the config object with the parameters from config file
		
		outputPlansFilePath = Constants.MATSIM_CONFIG_DIRECTORY + Constants.GENERATED_PLANS_FILE_NAME;
		generateNewMATSimConfigFile(config);
		
	}

}

