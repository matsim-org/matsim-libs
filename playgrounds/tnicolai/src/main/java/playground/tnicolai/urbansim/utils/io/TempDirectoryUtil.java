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
package playground.tnicolai.urbansim.utils.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonUtilities;

/**
 * @author thomas
 *
 */
public class TempDirectoryUtil {
	
	// logger
	private static final Logger log = Logger.getLogger(TempDirectoryUtil.class);
	// storage for created custom directories
	private static ArrayList<File> tempDirectoryList = null;

	
	/**
	 * creates a custom temp directory
	 * @param customDirectory
	 * @return canonical path of the custom temp directory
	 */
	public static String createCustomTempDirectory(String customDirectory){
		
		log.info("Creating a custom temp directory");
		
		try {
			String tempPath = CommonUtilities.checkPathEnding( System.getProperty("java.io.tmpdir") );
			
			log.info("Creating directory \"" + customDirectory + "\" in temp path \"" + tempPath + "\".");
		
			File tempFile = new File(tempPath + customDirectory);
			
			// Add custom directory to the list for the cleaning up method
			if(tempDirectoryList == null)
				tempDirectoryList = new ArrayList<File>();
			tempDirectoryList.add( tempFile );
			
			log.info("Finished creating custom temp directory " + tempFile.getCanonicalPath());
			return tempFile.getCanonicalPath();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.equals("Creating custom temp directory faild."); 
		return null;
	}
	
	/**
	 * Removes the custom temp directories
	 */
	public static void cleaningUpCustomTempDirectories(){

		if(tempDirectoryList != null){
			log.info("Removing custom temp directories");
			try {
				for(File tempFile : tempDirectoryList)
					if(tempFile.exists()){
						log.info("Deleting : " + tempFile.getCanonicalPath());
						deleteDirectory(tempFile);
					}
				// also remove all file references from ArrayList
				tempDirectoryList.clear();
			} catch (IOException e) {e.printStackTrace();}
		}
		else
			log.info("No custom temp directory created.");
		log.info("Finished removing custom temp directories");
	}

	/**
	 * creates directories
	 * @param path
	 * @return
	 */
	public static boolean createDirectory(String path){
		log.info("Creating directory " + path);
		try {
			File f = new File(path);
			if(!f.exists())
				return f.mkdirs();
			else
				return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * removes a given directory
	 * @param directory
	 */
	public static void deleteDirectory(String directory){
		log.info("Removing " + directory + " directory");
		File tempDir = new File(directory);
		deleteDirectory(tempDir);
		log.info("Finished removing directory");
	}
	
	/**
	 * recursive deletion of sub folders and files
	 * @param path
	 * @return
	 */
	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}
	
	/**
	 * create new temp directories. these will be deleted after each test run.
	 */
	public static void createOPUSDirectories(){
		log.info("Creating temp directories");
		
		// TODO for loop
		
		// set temp directory as opus_home
		Constants.setOpusHomeDirectory(System.getProperty("java.io.tmpdir"));

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
	public static void cleaningUpOPUSDirectories(){
		log.info("Removing temp directories");
		
		// TODO for loop

		File tempFile = new File(Constants.OPUS_HOME);
		if(tempFile.exists())
			deleteDirectory(tempFile);
		tempFile = new File(Constants.OPUS_MATSIM_DIRECTORY);
		if(tempFile.exists())
			deleteDirectory(tempFile);
		tempFile = new File(Constants.OPUS_MATSIM_OUTPUT_DIRECTORY);
		if(tempFile.exists())
			deleteDirectory(tempFile);
		tempFile = new File(Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY);
		if(tempFile.exists())
			deleteDirectory(tempFile);
		tempFile = new File(Constants.MATSIM_CONFIG_DIRECTORY);
		if(tempFile.exists())
			deleteDirectory(tempFile);	
		log.info("Finished removing temp directories");
	}

}

