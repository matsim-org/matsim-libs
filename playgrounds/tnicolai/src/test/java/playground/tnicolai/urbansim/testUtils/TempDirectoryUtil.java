/* *********************************************************************** *
 * project: org.matsim.*
 * TempDirectoryUtil.java
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
package playground.tnicolai.urbansim.testUtils;

import java.io.File;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class TempDirectoryUtil {
	
	private static final Logger log = Logger.getLogger(TempDirectoryUtil.class);
	
	/**
	 * create new temp directories. these will be deleted after each test run.
	 */
	public static void createDirectories(){
		log.info("Creating temp directories");

		File tempFile = new File(Constants.OPUS_HOME);
		tempFile.mkdirs();
		
		tempFile = new File(Constants.OPUS_MATSIM_DIRECTORY);
		tempFile.mkdirs();
		
		tempFile = new File(Constants.OPUS_MATSIM_OUTPUT_DIRECTORY);
		tempFile.mkdirs();
		
		tempFile = new File(Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY);
		tempFile.mkdirs();
		
		tempFile = new File(Constants.MATSIM_CONFIG_DIRECTORY);
		tempFile.mkdirs();
		log.info("Finished creating temp directories");
	}
	
	/**
	 * Removes the output directory for the UrbanSim data
	 * if doesn't existed before the test run. 
	 */
	public static void cleaningUp(){
		log.info("Removing temp directories");

		File tempFile = new File(Constants.OPUS_HOME);
		if(tempFile.exists())
			tempFile.delete();
		tempFile = new File(Constants.OPUS_MATSIM_DIRECTORY);
		if(tempFile.exists())
			tempFile.delete();
		tempFile = new File(Constants.OPUS_MATSIM_OUTPUT_DIRECTORY);
		if(tempFile.exists())
			tempFile.delete();
		tempFile = new File(Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY);
		if(tempFile.exists())
			tempFile.delete();
		tempFile = new File(Constants.MATSIM_CONFIG_DIRECTORY);
		if(tempFile.exists())
			tempFile.delete();		
		log.info("Finished removing temp directories");
	}

}

