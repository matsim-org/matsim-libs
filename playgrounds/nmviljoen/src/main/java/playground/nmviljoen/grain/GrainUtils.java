/* *********************************************************************** *
 * project: org.matsim.*
 * GrainUtils.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.nmviljoen.grain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * A number of utilities used for the grain study.
 * 
 * @author jwjoubert
 */
public class GrainUtils {
	final private static Logger LOG = Logger.getLogger(GrainUtils.class);

	/**
	 * Hide the constructor.
	 */
	private GrainUtils() {
	}
	
	/**
	 * Returns the twelve (12) monthly files, each containing the raw GPS 
	 * traces provided by Digicore in the longitudinal data set. The twelve 
	 * months are hard-coded: March 2013 to February 2014.
	 * 
	 * @param inputFolder
	 * @return
	 */
	public static List<File> getRawGpsTraceFiles(String inputFolder){
		LOG.info("Retrieveing the twelve monthly GPS trace files...");
		List<File> files = new ArrayList<File>();
		
		String[] months = getMonths();
		
		/* Check that each file exists, and once checked, add to list. */
		for(String month : months){
			File file = new File(inputFolder + (inputFolder.endsWith("/") ? "" : "/") + month + ".csv.gz");
			if(!file.exists()){
				LOG.error("The file " + file.getAbsolutePath() + " does not exist, and will be ignored.");
			} else{
				files.add(file);
			}
		}

		LOG.info("Done retrieving the twelve monthly files.");
		return files;
	}
	
	/**
	 * Returns the list of 'Vehicles' folders, each containing the vehicle 
	 * files that was split, and which now must be sorted.
	 * 
	 * @param processedFolder
	 * @return
	 */
	public static List<File> getVehicleFolders(String processedFolder){
		LOG.info("Retrieveing the twelve monthly GPS trace files...");
		List<File> files = new ArrayList<File>();
		
		String[] months = getMonths();
		for(String month : months){
			File folder = new File(processedFolder + (processedFolder.endsWith("/") ? "" : "/") + month + "/Vehicles/");
			if(!folder.exists()){
				LOG.error("The file " + folder.getAbsolutePath() + " does not exist, and will be ignored.");
			} else{
				files.add(folder);
			}
		}

		LOG.info("Done retrieving the vehicle files.");
		return files;
	}
	
	/**
	 * Returns the list of monthly folders containing the <code>./Vehicles/</code>
	 * and <code>./xml/</code> folders.
	 * 
	 * @param processedFolder
	 * @return
	 */
	public static List<File> getMonthlyOutputFolders(String processedFolder){
		LOG.info("Retrieveing the twelve monthly output folders...");
		List<File> files = new ArrayList<File>();
		
		String[] months = getMonths();
		for(String month : months){
			File folder = new File(processedFolder + (processedFolder.endsWith("/") ? "" : "/") + month + "/");
			if(!folder.exists()){
				LOG.error("The file " + folder.getAbsolutePath() + " does not exist, and will be ignored.");
			} else{
				files.add(folder);
			}
		}
		
		LOG.info("Done retrieving the output folders.");
		return files;
	}
	
	/**
	 * Returns the twelve months of March 2013 to February 2014.
	 * @return
	 */
	private static String[] getMonths(){
		String[] months = {
				"201303", "201304", "201305", "201306",
				"201307", "201308", "201309", "201310",
				"201311", "201312", "201401", "201402"
		};
		return months;
	}
	
}
