/* *********************************************************************** *
 * project: org.matsim.*
 * Constants.java
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
package playground.tnicolai.urbansim.constants;

import org.apache.log4j.Logger;

/**
 * @author thomas
 *
 */
public class Constants {
	
	private static final Logger log = Logger.getLogger(Constants.class);
	
	/** important system environments */
	public static String OPUS_HOME;// = System.getenv("OPUS_HOME");			// provided by UrbanSim via the configuration file; this is 
	
	/** subdirectories in OPUS_HOME */
	public static String OPUS_MATSIM_DIRECTORY;// = System.getenv("OPUS_HOME")+'/'+"opus_matsim/";						// TODO: these paths should be provided by UrbanSim only -> extend MATSim4UrbanSim config
	public static String MATSIM_CONFIG_DIRECTORY;// = System.getenv("OPUS_HOME")+'/'+"opus_matsim/matsim_config/";		// 		 a method to set those paths is already been implemented (setOpusHomeDirectory)
	public static String OPUS_MATSIM_OUTPUT_DIRECTORY;// = System.getenv("OPUS_HOME")+'/'+"opus_matsim/output/";
	public static String OPUS_MATSIM_TEMPORARY_DIRECTORY;// = System.getenv("OPUS_HOME")+'/'+"opus_matsim/tmp/";
	
	/**
	 * Apply a new root path for the OPUS_HOME directory
	 * @param opusHome path to the new OPUS_HOME Directory
	 */
	public static void setOpusHomeDirectory(String opusHome){
		
		if(!opusHome.endsWith("/"))
			opusHome += "/";
		
		OPUS_HOME = opusHome;
		OPUS_MATSIM_DIRECTORY = opusHome+"opus_matsim/";
		MATSIM_CONFIG_DIRECTORY = opusHome+"opus_matsim/matsim_config/";
		OPUS_MATSIM_OUTPUT_DIRECTORY = opusHome+"opus_matsim/output/";
		OPUS_MATSIM_TEMPORARY_DIRECTORY = opusHome+"opus_matsim/tmp/";
		
		log.info("");
		log.info("Set OPUS_HOME to :" + OPUS_HOME);
		log.info("Set OPUS MATSim directory to :" + OPUS_MATSIM_DIRECTORY);
		log.info("Set MATSim config directory to :" + MATSIM_CONFIG_DIRECTORY);
		log.info("Set OPUS MATSim output directory to :" + OPUS_MATSIM_OUTPUT_DIRECTORY);
		log.info("Set OPUS MATSim temp directory to :" + OPUS_MATSIM_TEMPORARY_DIRECTORY);
		log.info("");
	}
	
	/** subdirectories in MATSim */
	public static final String MATSIM_WORKING_DIRECTORY = System.getProperty("user.dir");
	
	/** file names */
	public static final String GENERATED_PLANS_FILE_NAME = "output_plans.xml.gz";
	public static final String GENERATED_MATSIM_CONFIG_FILE_NAME = "MATSimConfigFile.xml.gz";
	public static final String URBANSIM_PARCEL_DATASET_TABLE = "parcel__dataset_table__exported_indicators__";
	public static final String URBANSIM_PERSON_DATASET_TABLE = "person__dataset_table__exported_indicators__";
	
	/** file type */
	public static final String FILE_TYPE_TAB = ".tab";
	public static final String FILE_TYPE_CSV = ".csv";
	public static final String FILE_TYPE_DBL = ".dbl";
	public static final String FILE_TYPE_ESRI = ".esri";
	public static final String FILE_TYPE_GZ = ".gz";
	public static final String FILE_TYPE_XML = ".xml";
	
	/** parameter for computing urbansim data */
	public static final String TAB_SEPERATOR = "[\t\n]+";
	public static final String TAB = "\t";
	public static final String NEW_LINE	= "\r\n";
	public static final String PARCEL_ID = "parcel_id";
	public static final String PERSON_ID = "person_id";
	public static final String PARCEL_ID_HOME = "parcel_id_home";
	public static final String PARCEL_ID_WORK = "parcel_id_work";
	public static final String X_COORDINATE = "x_coord_sp";
	public static final String Y_COORDINATE = "y_coord_sp";
	public static final String ZONE_ID = "zone_id";
	public static final String FACILITY_DESCRIPTION = "urbansim location";
	public static final String ACT_HOME = "home";
	public static final String ACT_WORK = "work";
	
	/** debugging flags */
	public static final String DEFAULT_MATSIM_CONFIG_FILE = "/Users/thomas/Development/opus_home/opus_matsim/matsim_config/seattle_matsim_0.xml";
	/** test run */
	public static final int TEST_RUN_SUCCESSFUL = 0;
	public static final int TEST_RUN_FAILD = -1;
	
	/** measurements */
	public static final String MEASUREMENT_LOGFILE = "psrc_log.txt";
	
	/** xsd on matsim.org */
	public static final String MATSim_4_UrbanSim_XSD = "http://www.matsim.org/files/dtd/MATSim4UrbanSimConfigSchema.xsd";
	
	/** MATSim properties */
	public static final String MATSIM_PROPERTIES_FILE = "matsim.properties";
	public static final String FIRST_ITERATION = "firstIteration";
	public static final String LAST_ITERATION = "lastIteration";
	public static final String NUMBER_OF_ITERATIONS_PER_RUN = "numberOfIterationPerRun";
	public static final String PLANS_FILE_PATH = "inputPlansFile";
	public static final String RUN_NUMBER = "run";
	public static final String PATH_TO_GENERATED_MATSIM_CONFIG_FILE = "generatedMATSimConfigPath";
	
	/** MATSim config modules */
	public static final String MATSIM_CONFIG_MODULE_CONTROLLER = "controler";
	public static final String MATSIM_CONFIG_MODULE_PLANS = "plans";
	public static final String MATSIM_CONFIG_MODULE_URBANSIM_PARAMETER = "urbansimParameter";
	public static final String MATSIM_CONFIG_PARAMETER_SAMPLING_RATE = "samplingRate";
	public static final String MATSIM_CONFIG_PARAMETER_YEAR = "year";
	public static final String MATSIM_CONFIG_PARAMETER_TEMP_DIRECTORY = "tempDirectory";
	
	/** exit codes */
	public static final int NOT_VALID_PATH	= 0;
	public static final int MATSIM_PROPERTIES_FILE_NOT_FOUND	= 1;
	public static final int CONFIG_OBJECT_NOT_INITIALIZED = 2;
	public static final int EXCEPTION_OCCURED	= 3;
	public static final int UNMARSCHALLING_FAILED	= 4;
	
}

