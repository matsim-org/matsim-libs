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
package org.matsim.contrib.matsim4urbansim.constants;

import org.apache.log4j.Logger;


/**
 * @author thomas
 *
 */
public class InternalConstants {
	
	// logger
	private static final Logger log = Logger.getLogger(InternalConstants.class);
	
	/** important system environments */
	private static String OPUS_HOME;
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
		
		setOPUS_HOME(opusHome);
		OPUS_DATA_PATH = opusHome+"data/";
		MATSIM_4_OPUS = opusHome+"opus_matsim/";
		MATSIM_4_OPUS_CONFIG = opusHome+"opus_matsim/matsim_config/";
		MATSIM_4_OPUS_OUTPUT = opusHome+"opus_matsim/output/";
		MATSIM_4_OPUS_TEMP = opusHome+"opus_matsim/tmp/";
		MATSIM_4_OPUS_BACKUP = opusHome+"opus_matsim/backup/";
		
		log.info("");
		log.info("Set OPUS_HOME to :" + getOPUS_HOME());
		log.info("Set OPUS_DATA_PATH to :" + OPUS_DATA_PATH);
		log.info("Set OPUS MATSim directory to :" + MATSIM_4_OPUS);
		log.info("Set MATSim config directory to :" + MATSIM_4_OPUS_CONFIG);
		log.info("Set OPUS MATSim output directory to :" + MATSIM_4_OPUS_OUTPUT);
		log.info("Set OPUS MATSim temp directory to :" + MATSIM_4_OPUS_TEMP);
		log.info("Set OPUS MATSim backup directory to :" + MATSIM_4_OPUS_BACKUP);
		log.info("");
	}
	
	public static void setOPUS_HOME(String oPUS_HOME) {
		OPUS_HOME = oPUS_HOME;
	}
	public static String getOPUS_HOME() {
		return OPUS_HOME;
	}

	/** subdirectories in MATSim */
	public static String MATSIM_WORKING_DIRECTORY = System.getProperty("user.dir");
	
	/** file names */
	public static final String GENERATED_PLANS_FILE_NAME = "output_plans.xml.gz";
	public static final String URBANSIM_PARCEL_DATASET_TABLE = "parcel__dataset_table__exported_indicators__";
	public static final String URBANSIM_ZONE_DATASET_TABLE = "zone__dataset_table__exported_indicators__";
	public static final String URBANSIM_PERSON_DATASET_TABLE = "person__dataset_table__exported_indicators__";
	public static final String URBANSIM_JOB_DATASET_TABLE = "job__dataset_table__exported_indicators__";
	
	/** file type */
	public static final String FILE_TYPE_TAB = ".tab";
	public static final String FILE_TYPE_TXT = ".txt";

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
	public static final String ACT_HOME = "home";
	public static final String ACT_WORK = "work";
	
	/** xsd on matsim.org */
	public static final String CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v3.xsd";
	public static final String CURRENT_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v3.xsd";
	public static final String CURRENT_XSD_FILE_NAME = "matsim4urbansim_v3.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_MATSIMORG = "http://matsim.org/files/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_SOURCEFOREGE = "https://matsim.svn.sourceforge.net/svnroot/matsim/matsim/trunk/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_MATSIM_4_URBANSIM_XSD_LOCALJAR = "/dtd/matsim4urbansim_v2.xsd";
	public static final String V2_XSD_FILE_NAME = "matsim4urbansim_v2.xsd";
	public static final String JAXB_PARSER_PACKAGE_NAME = "matsim4urbansim.jaxbconfigv3";	
}

