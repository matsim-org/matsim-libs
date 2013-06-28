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
package playground.ikaddoura.optimization.io;

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
	                	
	                	if (parts[0].equals("startHeadway")){
	                		this.optSettings.setStartHeadway(Double.parseDouble(parts[1]));
	                	
	                	} else if (parts[0].equals("startFare")){
	                		this.optSettings.setStartFare(Double.parseDouble(parts[1]));
	                		                	
	            		} else if (parts[0].equals("startCapacity")){
	                		this.optSettings.setStartCapacity(Integer.parseInt(parts[1]));
	                		
	            		} else if (parts[0].equals("startDemand")){
	                		this.optSettings.setStartDemand(Integer.parseInt(parts[1]));
	            			
	            		} else if (parts[0].equals("incrHeadway")){
	                		this.optSettings.setIncrHeadway(Double.parseDouble(parts[1]));
	                		
	                	} else if (parts[0].equals("incrFare")){
	                		this.optSettings.setIncrFare(Double.parseDouble(parts[1]));
	                		
	                	} else if (parts[0].equals("incrCapacity")){
	                		this.optSettings.setIncrCapacity(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("incrDemand")){
	                		this.optSettings.setIncrDemand(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("stepsHeadway")){
		                	this.optSettings.setStepsHeadway(Integer.parseInt(parts[1]));
		                		
	                	} else if (parts[0].equals("stepsFare")){
	                		this.optSettings.setStepsFare(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("stepsCapacity")){
	                		this.optSettings.setStepsCapacity(Integer.parseInt(parts[1]));
	                	
	                	} else if (parts[0].equals("stepsDemand")){
	                		this.optSettings.setStepsDemand(Integer.parseInt(parts[1]));
	                		
	                	} else if (parts[0].equals("useRandomSeedsFile")){
	                		this.optSettings.setUseRandomSeedsFile(Boolean.parseBoolean(parts[1]));
	                		
	                	} else if (parts[0].equals("usePopulationPathsFile")){
	                		this.optSettings.setUsePopulationPathsFile(Boolean.parseBoolean(parts[1]));
	                	
	                	} else if (parts[0].equals("randomSeedsFile")){
	                		this.optSettings.setRandomSeedsFile(parts[1]);
	                	
	                	} else if (parts[0].equals("populationPathsFile")){
	                		this.optSettings.setPopulationPathsFile(parts[1]);
	                		
	                	} else if (parts[0].equals("calculate_inVehicleTimeDelayEffects")){
	                		this.optSettings.setCalculating_inVehicleTimeDelayEffects(Boolean.parseBoolean(parts[1]));
	                		
	                	} else if (parts[0].equals("calculate_waitingTimeDelayEffects")){
	                		this.optSettings.setCalculating_waitingTimeDelayEffects(Boolean.parseBoolean(parts[1]));
	                	
	                	} else if (parts[0].equals("calculate_capacityDelayEffects")){
	                		this.optSettings.setCalculate_capacityDelayEffects(Boolean.parseBoolean(parts[1]));
	                		
	                	} else if (parts[0].equals("marginalCostPricingPt")){
	                		this.optSettings.setMarginalCostPricingPt(Boolean.parseBoolean(parts[1]));
	                	
	                	} else if (parts[0].equals("calculate_carCongestionEffects")){
	                		this.optSettings.setCalculate_carCongestionEffects(Boolean.parseBoolean(parts[1]));
	                		
	                	} else if (parts[0].equals("marginalCostPricingCar")){
	                		this.optSettings.setMarginalCostPricingCar(Boolean.parseBoolean(parts[1]));
	                	
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
