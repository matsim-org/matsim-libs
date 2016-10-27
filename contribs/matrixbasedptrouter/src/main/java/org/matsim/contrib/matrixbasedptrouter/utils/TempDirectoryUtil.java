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
package org.matsim.contrib.matrixbasedptrouter.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;

import org.apache.log4j.Logger;



/**
 * @author thomas
 *
 * @deprecated To get a temporary directory for production, use {@link java.nio.file.Files#createTempDirectory(Path, String, FileAttribute[])}}.
 * For tests, use the JUnit4 TemporaryFolder Rule.
 * <br>
 * (This class is now using nio.file.Files#..., so maybe it is ok to leave it as it is as a wrapper. kai, aug'15)
 *
 */
@Deprecated
public final class TempDirectoryUtil {
	
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
			Path tempDirectory = Files.createTempDirectory(customDirectory);
			File tempFile = tempDirectory.toFile();

			// Add custom directory to the list for the cleaning up method
			if(tempDirectoryList == null)
				tempDirectoryList = new ArrayList<File>();
			tempDirectoryList.add( tempFile );
			
			createDirectory(tempFile.getCanonicalPath());
			
			log.info("Finished creating custom temp directory " + tempFile.getCanonicalPath());
			return tempFile.getCanonicalPath();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Removes the custom temp directories
	 */
	public static void cleanUpCustomTempDirectories(){

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
	 * makes sure that a path ends with "/"
	 * @param any desired path
	 * @return path that ends with "/"
	 */
	public static String checkPathEnding(String path){
		
		path.replace('\\', '/');
		
		if(path.endsWith("/"))
			return path;
		else
			return path + "/";
	}
	
	/**
	 * Checks if a given path exists
	 * @param arg path
	 * @return true if the given file exists
	 */
	 public static boolean pathExists(String path){
		return( (new File(path)).exists() );
	}
}

