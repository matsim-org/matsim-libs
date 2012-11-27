/* *********************************************************************** *
 * project: org.matsim.*
 * PRfileReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.busCorridorPaper.busCorridorWelfareAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * @author ikaddoura
 *
 */
public class OptSettingsReader {
	private static final Logger log = Logger.getLogger(OptSettingsReader.class);
	private OptSettings optSettings = new OptSettings();
	private String settingsFile;

	public OptSettingsReader(String settingsFile) {
		this.settingsFile = settingsFile;
		log.info("Optimization settings file set to " + this.settingsFile);
	}

	public OptSettings getOptSettings() {
		
		log.info("Reading optimization settings from file...");
		
		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(this.settingsFile)));
	            String line = null;
	            while((line = br.readLine()) != null) {
	                	String[] parts = line.split(";");
	                	
	                	if (parts[0].equals("configFile")){
	                		this.optSettings.setConfigFile(parts[1]);
	                		
	                	} else if (parts[0].equals("outputPath")){
	                		this.optSettings.setOutputPath(parts[1]);
	                		
	                	} else if (parts[0].equals("optimizationParameter1")){
	                		this.optSettings.setOptimizationParameter1(parts[1]);
	                		
	                	} else if (parts[0].equals("optimizationParameter2")){
	                		this.optSettings.setOptimizationParameter2(parts[1]);
	                		
	                	} else if (parts[0].equals("lastExtIt1")){
	                		this.optSettings.setLastExtIt1(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("lastExtIt2")){
	                		this.optSettings.setLastExtIt2(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("startBusNumber")){
	                		this.optSettings.setStartBusNumber(Integer.parseInt(parts[1]));
	                	
	                	} else if (parts[0].equals("startFare")){
	                		this.optSettings.setStartFare(Double.parseDouble(parts[1]));
	                		                	
	            		} else if (parts[0].equals("startCapacity")){
	                		this.optSettings.setStartCapacity(Integer.parseInt(parts[1]));
	            			
	            		} else if (parts[0].equals("incrBusNumber")){
	                		this.optSettings.setIncrBusNumber(Integer.parseInt(parts[1]));
	                		if (this.optSettings.getIncrBusNumber() == 0){
	    						log.info("incrBusNumber is set to 0. Using standard values...");
	                		}
	                		
	                	} else if (parts[0].equals("incrFare")){
	                		this.optSettings.setIncrFare(Double.parseDouble(parts[1]));
	                		
	                	} else if (parts[0].equals("incrCapacity")){
	                		this.optSettings.setIncrCapacity(Integer.parseInt(parts[1]));
	                	
	            		} else {
	            			throw new RuntimeException(parts[0] +" is an unknown parameter in the optimization settings file. Aborting...");
	            		}
	               
	                }
	        } catch(FileNotFoundException e) {
	            e.printStackTrace();
	        } catch(IOException e) {
	            e.printStackTrace();
	        } finally {
	            if(br != null) {
	                try {
	                    br.close();
	                } catch(IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        log.info("Reading optimization settings from file... Done.");
			return this.optSettings;
	}
}
