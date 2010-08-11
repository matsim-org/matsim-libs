/* *********************************************************************** *
 * project: org.matsim.*
 * TempDirectory.java
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
package playground.tnicolai.urbansim.utils.archive;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * @author thomas
 *
 */
public class TempDirectory {
	
	// logger
	private static final Logger log = Logger.getLogger(TempDirectory.class);
	
	private static TempDirectory singelton = null;
	private static File tempDirectory = null;
	
	/**
	 * Private constructor to get a singelton instance
	 */
	private TempDirectory(){
		try{
			log.info("Creating temp directory...");
			tempDirectory = File.createTempFile("temp", Long.toString(System.nanoTime()));
			tempDirectory.mkdirs();
			log.info("Created " + tempDirectory.getCanonicalPath());
		}
		catch (IOException ioe) {
			log.error("Error while initilizing TempDirectory");
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Indicates whether singelton instance is null
	 * @return boolean true if singelton instance is null, false otherwise
	 */
	public static boolean isSingeltonInstanceNull(){
		return (singelton == null);
	}
	
	/**
	 * Get singelton instance of TempDirectory
	 * @return pointer to TempDirectory
	 */
	public static TempDirectory getTempDirectoryInstance(){
		if(singelton == null)
			singelton = new TempDirectory();
		return singelton;
	}
	
	/**
	 * Returns canonical path of temp directory
	 * @return String canonical path
	 */
	public static String getTempDirectoryPath(){
		try {
			return tempDirectory.getCanonicalPath();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Removing temp directory
	 */
	public static void destroyTempDirectory(){
		log.info("Deleting temp directory");
		tempDirectory.delete();
		log.info("Deleting finished");
	}

}

