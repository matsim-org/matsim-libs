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
package playground.andreas.aas.modules.cellBasedAccessibility.constants;

import org.apache.log4j.Logger;


/**
 * @author thomas
 *
 */
public class InternalConstants {
	
	// logger
	private static final Logger log = Logger.getLogger(InternalConstants.class);
	
	/** important system environments */
	public static String OPUS_HOME;
	public static String OPUS_DATA_PATH;
	
	/** subdirectories in OPUS_HOME */
	public static String MATSIM_4_OPUS; 		// OPUS_HOME/matsim4opus/
	public static String MATSIM_4_OPUS_CONFIG;	// OPUS_HOME/matsim4opus/matsim_config/
	public static String MATSIM_4_OPUS_OUTPUT;	// OPUS_HOME/matsim4opus/output/
	public static String MATSIM_4_OPUS_TEMP;	// OPUS_HOME/matsim4opus/tmp/
	public static String MATSIM_4_OPUS_BACKUP;	// OPUS_HOME/matsim4opus/backup
	
	/**
	 * Apply a new root path for the OPUS_HOME directory
	 * @param opusHome path to the new OPUS_HOME Directory
	 */
	public static void setOpusHomeDirectory(String opusHome){
		
		if(!opusHome.endsWith("/"))
			opusHome += "/";
		
		OPUS_HOME = opusHome;
		OPUS_DATA_PATH = opusHome+"data/";
		MATSIM_4_OPUS = opusHome+"opus_matsim/";
		MATSIM_4_OPUS_CONFIG = opusHome+"opus_matsim/matsim_config/";
		MATSIM_4_OPUS_OUTPUT = opusHome+"opus_matsim/output/";
		MATSIM_4_OPUS_TEMP = opusHome+"opus_matsim/tmp/";
		MATSIM_4_OPUS_BACKUP = opusHome+"opus_matsim/backup/";
		
		log.info("");
		log.info("Set OPUS_HOME to :" + OPUS_HOME);
		log.info("Set OPUS_DATA_PATH to :" + OPUS_DATA_PATH);
		log.info("Set OPUS MATSim directory to :" + MATSIM_4_OPUS);
		log.info("Set MATSim config directory to :" + MATSIM_4_OPUS_CONFIG);
		log.info("Set OPUS MATSim output directory to :" + MATSIM_4_OPUS_OUTPUT);
		log.info("Set OPUS MATSim temp directory to :" + MATSIM_4_OPUS_TEMP);
		log.info("Set OPUS MATSim backup directory to :" + MATSIM_4_OPUS_BACKUP);
		log.info("");
	}
	
	/** subdirectories in MATSim */
	public static final String MATSIM_WORKING_DIRECTORY = System.getProperty("user.dir");
	
	/** file names */
	public static final String GENERATED_PLANS_FILE_NAME = "output_plans.xml.gz";
	public static final String SCORESTATS_FILE_NAME = "scorestats.txt";
	public static final String TRAVELDISTANCESSTAT_FILE_NAME = "traveldistancestats.txt";
	public static final String LOG_FILE_NAME = "logfile.log";
	public static final String LOG_FILE_WARNINGS_ERRORS_NAME = "logfileWarningsErrors.log";
	public static final String OUTPUT_CONFIG_FILE_NAME = "output_config.xml.gz";
	public static final String OUTPUT_NETWORK_FILE_NAME = "output_network.xml.gz";
	public static final String GENERATED_MATSIM_CONFIG_FILE_NAME = "MATSimConfigFile.xml.gz";
	public static final String URBANSIM_PARCEL_DATASET_TABLE = "parcel__dataset_table__exported_indicators__";
	public static final String URBANSIM_ZONE_DATASET_TABLE = "zone__dataset_table__exported_indicators__";
	public static final String URBANSIM_PERSON_DATASET_TABLE = "person__dataset_table__exported_indicators__";
	public static final String URBANSIM_JOB_DATASET_TABLE = "job__dataset_table__exported_indicators__";
	
	/** file type */
	public static final String FILE_TYPE_TAB = ".tab";
	public static final String FILE_TYPE_CSV = ".csv";
	public static final String FILE_TYPE_DBL = ".dbl";
	public static final String FILE_TYPE_ESRI = ".esri";
	public static final String FILE_TYPE_GZ  = ".gz";
	public static final String FILE_TYPE_XML = ".xml";
	public static final String FILE_TYPE_KMZ = ".kmz";
	public static final String FILE_TYPE_TXT = ".txt";
	
	/** matsim output files */
	public static final String OUTPUT_PLANS_FILE_GZ 	= "output_plans.xml.gz";
	public static final String OUTPUT_PLANS_FILE_XML 	= "output_plans.xml";
	public static final String OUTPUT_PARCEL_FILE_GZ	= "parcels.xml.gz";
	public static final String OUTPUT_PARCEL_FILE_XML	= "parcels.xml";
	public static final String OUTPUT_ZONES_FILE_GZ	 	= "zones.xml.gz";
	public static final String OUTPUT_ZONES_FILE_XML	= "zones.xml";
	public static final String TRAVEL_DATA_FILE_CSV		= "travel_data.csv";
	public static final String GRID_DATA_FILE_CSV 		= "grid_data.csv";
	public static final String ZONES_COMPLETE_FILE_CSV 	= "zones_complete.csv";

	/** parameter for computing urbansim data */
	public static final String TAB_SEPERATOR = "[\t\n]+";
	public static final String TAB = "\t";
	public static final String NEW_LINE	= "\r\n";
	public static final String PERSON_ID = "person_id";
	public static final String JOB_ID = "job_id";
	public static final String PARCEL_ID = "parcel_id";
	public static final String PARCEL_ID_HOME = "parcel_id_home";
	public static final String PARCEL_ID_WORK = "parcel_id_work";
	public static final String ZONE_ID = "zone_id";
	public static final String ZONE_ID_HOME = "zone_id_home";
	public static final String ZONE_ID_WORK = "zone_id_work";
	public static final String ZONE_CENTROID_X_COORD = "x_coord_zonecentroid";
	public static final String ZONE_CENTROID_Y_COORD = "y_coord_zonecentroid";
	public static final String NEARESTNODE_ID = "nearest_node_id";
	public static final String NEARESTNODE_X_COORD = "x_coord_nn";
	public static final String NEARESTNODE_Y_COORD = "y_coord_nn";
	public static final String X_COORDINATE_SP = "x_coord_sp";
	public static final String Y_COORDINATE_SP = "y_coord_sp";
	public static final String X_COORDINATE = "xcoord";
	public static final String Y_COORDINATE = "ycoord";
	public static final String FACILITY_DESCRIPTION = "urbansim location";
	public static final String ACT_HOME = "home";
	public static final String ACT_WORK = "work";
	
	/** UrbanSim and/or MATSim output file header items */
	public static final String ACCESSIBILITY_BY_FREESPEED = "freespeed_accessibility";
	public static final String ACCESSIBILITY_BY_CAR = "car_accessibility";
	public static final String ACCESSIBILITY_BY_BIKE = "bike_accessibility";
	public static final String ACCESSIBILITY_BY_WALK = "walk_accessibility";
	public static final String TRAVEL_TIME_ACCESSIBILITY = "travel_time_accessibility";
	public static final String TRAVEL_COST_ACCESSIBILITY = "travel_cost_accessibility";
	public static final String TRAVEL_DISTANCE_ACCESSIBILITY = "travel_distance_accessibility";
	public static final String CONGESTED_TRAVEL_TIME_ACCESSIBILITY = "congested_travel_time_accessibility";
	public static final String FREESPEED_TRAVEL_TIME_ACCESSIBILITY = "freespeed_travel_time_accessibility";
	public static final String WALK_TRAVEL_TIME_ACCESSIBILITY = "walk_travel_time_accessibility";
	public static final String PERSONS_COUNT = "persons";
	public static final String WORKPLACES_COUNT = "workplaces";
	
	/** xsd on matsim.org */
	public static final String CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v1.xsd";
	public static final String CURRENT_MATSIM_4_URBANSIM_XSD_SOURCEFOREGE = "https://matsim.svn.sourceforge.net/svnroot/matsim/matsim/trunk/dtd/matsim4urbansim_v1.xsd";
	public static final String CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v1.xsd";
	public static final String CURRENT_XSD_FILE_NAME = "matsim4urbansim_v1.xsd";
	public static final String V1_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v1.xsd";
	public static final String V1_MATSIM_4_URBANSIM_XSD_SOURCEFOREGE = "https://matsim.svn.sourceforge.net/svnroot/matsim/matsim/trunk/dtd/matsim4urbansim_v1.xsd";
	public static final String V1_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v1.xsd";
	public static final String V1_XSD_FILE_NAME = "matsim4urbansim_v1.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_SOURCEFOREGE = "https://matsim.svn.sourceforge.net/svnroot/matsim/matsim/trunk/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_XSD_FILE_NAME = "matsim4urbansim_v2.xsd";
	public static final String V3_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v3.xsd";
	public static final String V3_MATSIM_4_URBANSIM_XSD_SOURCEFOREGE = "https://matsim.svn.sourceforge.net/svnroot/matsim/matsim/trunk/dtd/matsim4urbansim_v3.xsd";
	public static final String V3_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v3.xsd";
	public static final String V3_XSD_FILE_NAME = "matsim4urbansim_v3.xsd";
	public static final String JAXB_PARSER_PACKAGE_NAME = "matsim4urbansim.jaxbconfig";
	
	/** MATSim properties */
	public static final String MATSIM_PROPERTIES_FILE = "matsim.properties";
	
	/** MATSim config modules */
	public static final String MATSIM_CONFIG_MODULE_CONTROLLER = "matsimControler";
	public static final String MATSIM_CONFIG_MODULE_PLANS = "plans";
	public static final String MATSIM_CONFIG_MODULE_URBANSIM_PARAMETER = "urbansimParameter";
	public static final String MATSIM_CONFIG_PARAMETER_SAMPLING_RATE = "samplingRate";
	public static final String MATSIM_CONFIG_PARAMETER_YEAR = "year";
	public static final String MATSIM_CONFIG_PARAMETER_TEMP_DIRECTORY = "tempDirectory";
	
	/** exit codes */
	public static final int NOT_VALID_PATH		= 0;
	public static final int MATSIM_PROPERTIES_FILE_NOT_FOUND = 1;
	public static final int EXCEPTION_OCCURED	= 2;
	public static final int UNMARSCHALLING_FAILED = 3;
	public static final int NO_MATSIM_NETWORK = 4;
	
	/** MATSim 4 UrbanSim (urbansimParameter) parameter names **/
	public static final String URBANSIM_PARAMETER = "urbansimParameter";
	public static final String IS_TEST_RUN = "isTestRun";
	public static final String SAMPLING_RATE = "samplingRate";
	public static final String OPUS_HOME_PARAM = "opusHomeParam";
	public static final String OPUS_DATA_PATH_PARAM = "opusDataPathParam";
	public static final String MATSIM_4_OPUS_PARAM = "matsim4OpusParam";
	public static final String MATSIM_4_OPUS_CONFIG_PARAM = "matsim4OpusConfigParam";
	public static final String MATSIM_4_OPUS_OUTPUT_PARAM = "matsim4OpusOutputParam";
	public static final String MATSIM_4_OPUS_TEMP_PARAM = "matsim4OpusTempParam";
	public static final String MATSIM_4_OPUS_BACKUP_PARAM = "matsim4OpusBackupParam";
	public static final String YEAR = "year";
	public static final String BACKUP_RUN_DATA_PARAM = "backupRunDataParam";
	public static final String TEST_PARAMETER_PARAM = "testParameter";
	public static final String MEASUREMENT_LOGFILE = "psrc_log.txt";
	public static final String MATSIM_MODE = "matsim_mode";
	public static final String TARGET_LOCATION_HOT_START_PLANS_FILE = "target_location_for_hotstart_plans_file";
	
	/** MATSim 4 UrbanSim (accessibilityParameter) parameter names **/
	public static final String ACCESSIBILITY_PARAMETER = "accessibility_parameter";
	public static final String LOGIT_SCALE_PARAMETER = "logitScaleParameter"; // Formally known as "beta_brain"
	public static final String BETA_CAR_TRAVEL_TIMES = "betaCarTravelTime";
	public static final String BETA_CAR_LN_TRAVEL_TIMES = "betaCarLnTravelTime";
	public static final String BETA_CAR_TRAVEL_TIMES_POWER = "betaCarTravelTimePower2";
	public static final String BETA_CAR_TRAVEL_DISTANCE = "betaCarTravelDistance";
	public static final String BETA_CAR_LN_TRAVEL_DISTANCE = "betaCarLnTravelDistance";
	public static final String BETA_CAR_TRAVEL_DISTANCE_POWER = "betaCarTravelDistancePower2";
	public static final String BETA_CAR_TRAVEL_COSTS = "betaCarTravelCost";
	public static final String BETA_CAR_LN_TRAVEL_COSTS = "betaCarLnTravelCost";
	public static final String BETA_CAR_TRAVEL_COSTS_POWER = "betaCarTravelCostPower2";
	public static final String BETA_WALK_TRAVEL_TIMES = "betaWalkTravelTime";
	public static final String BETA_WALK_LN_TRAVEL_TIMES = "betaWalkLnTravelTime";
	public static final String BETA_WALK_TRAVEL_TIMES_POWER = "betaWalkTravelTimePower2";
	public static final String BETA_WALK_TRAVEL_DISTANCE = "betaWalkTravelDistance";
	public static final String BETA_WALK_LN_TRAVEL_DISTANCE = "betaWalkTravelDistancePower2";
	public static final String BETA_WALK_TRAVEL_DISTANCE_POWER = "betaWalkLnTravelDistance";
	public static final String BETA_WALK_TRAVEL_COSTS = "betaWalkTravelCost";
	public static final String BETA_WALK_LN_TRAVEL_COSTS = "betaWalkTravelCostPower2";
	public static final String BETA_WALK_TRAVEL_COSTS_POWER = "betaWalkLnTravelCost";
	
	/** Spatial IDs and spatial conversion factors */
	
	public static final String PROJECT_NAME_BRUSSELS_ZONE	= "brussels_zone";
	public static final String PROJECT_NAME_ZURICH_PARCEL 	= "zurich_parcel";
	public static final String PROJECT_NAME_SEATTLE_PARCEL	= "seattle_parcel";
	public static final String PROJECT_NAME_PSRC_PARCEL		= "psrc_parcel";	
	public static final int SRID_WASHINGTON_NORTH = 2926;	// srid 2285 also worked, but was last updated in 2001 instead of 2007
	public static final int SRID_SWITZERLAND = 21781;		// 
	public static final int SRID_BELGIUM = 31300;			// this is the EPSG id of Belgium (old id 31300, new id 3447)
	public static final double FEET_IN_METER_CONVERSION_FACTOR = 0.3048; 			// this means 1ft corresponds to 0.348m
	public static final double METER_IN_FEET_CONVERSION_FACTOR = 3.280839895013124;	// here 1 meter corresponds to 3.28084ft
	
	/** MATSim 4 UrbanSim parameter values as strings **/
	public static final String TRUE = "true";
	
	/** MATSim Modes **/
	public static final String COLD_START = "cold_start";
	public static final String WARM_START = "warm_start";
	public static final String HOT_START = "hot_start";
	
	/** MATSim4OPUS TEST data folder structure */
	public static final String MATSIM_TEST_DATA_WARM_START_URBANSIM_OUTPUT = "warmstart/urbanSimOutput";
	public static final String MATSIM_TEST_DATA_WARM_START_INPUT_PLANS = "warmstart/inputPlan";
	public static final String MATSIM_TEST_DATA_WARM_START_NETWORK = "warmstart/network";
	public static final String MATSIM_TEST_DATA_DEFAULT_URBANSIM_OUTPUT = "urbanSimOutput";
	
}

