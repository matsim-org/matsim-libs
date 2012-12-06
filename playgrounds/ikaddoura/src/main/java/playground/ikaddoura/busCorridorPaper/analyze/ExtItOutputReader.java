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
package playground.ikaddoura.busCorridorPaper.analyze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */
public class ExtItOutputReader {
	private static final Logger log = Logger.getLogger(ExtItOutputReader.class);
	private Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana = new HashMap<Integer, Map <Integer, ExtItAnaInfo>>();

	private String runFolder;

	public ExtItOutputReader(String runFolder) {
		this.runFolder = runFolder;
	}

	public void readData() {
		
		log.info("Loading run outputs from folder " + this.runFolder);
		File folder = new File(this.runFolder);
		int runNr = 0;
		for ( File file : folder.listFiles() ) {
			if (file.isDirectory() && !file.isHidden()){
				String runOutputFile = file.toString() + "/extItData.csv";
				loadRunOutputFile(runOutputFile, runNr);
				runNr++;
			}
		}
	}

	private void loadRunOutputFile(String runOutputFile, int runNr) {
		log.info("Loading run outputFile # " + runNr + ": " + runOutputFile.toString() + "...");
    	Map<Integer, ExtItAnaInfo> it2Ana = new HashMap<Integer, ExtItAnaInfo>();

		 BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(new File(runOutputFile)));
	            String line = null;
	            int lineCounter = 0;
	            while((line = br.readLine()) != null) {
	            		            	
	                if (lineCounter > 0) {
	                	String[] parts = line.split(";"); 
	                	ExtItAnaInfo extItAnaInfo = new ExtItAnaInfo();
	                	
	                	extItAnaInfo.setIteration(Integer.parseInt(parts[0]));
	                	extItAnaInfo.setNumberOfBuses((int) Double.parseDouble(parts[1]));
	                	extItAnaInfo.setFare(Double.parseDouble(parts[3]));
	                	extItAnaInfo.setCapacity(Double.parseDouble(parts[4]));
	                	extItAnaInfo.setOperatorProfit(Double.parseDouble(parts[7]));
	                	extItAnaInfo.setUsersLogSum(Double.parseDouble(parts[8]));
	                	extItAnaInfo.setWelfare(Double.parseDouble(parts[9]));
	                	extItAnaInfo.setNumberOfCarLegs(Double.parseDouble(parts[10]));
	                	extItAnaInfo.setNumberOfPtLegs(Double.parseDouble(parts[11]));
	                	extItAnaInfo.setNumberOfWalkLegs(Double.parseDouble(parts[12]));
	                	extItAnaInfo.setAvgWaitingTimeAll(Double.parseDouble(parts[13]));
	                	extItAnaInfo.setAvgWaitingTimeNotMissing(Double.parseDouble(parts[14]));
	                	extItAnaInfo.setAvgWaitingTimeMissing(Double.parseDouble(parts[15]));
	                	extItAnaInfo.setNumberOfMissedBusTrips(Double.parseDouble(parts[16]));
	                	extItAnaInfo.setNumberOfNotMissedBusTrips(Double.parseDouble(parts[17]));
	                	extItAnaInfo.setT0MinusTActSum(Double.parseDouble(parts[19]));
	                	extItAnaInfo.setAvgT0MinusTActPerPerson(Double.parseDouble(parts[20]));
	                	extItAnaInfo.setAvgT0MinusTActDivByT0perTrip(Double.parseDouble(parts[21]));
	                	extItAnaInfo.setNoValidPlanScore(Double.parseDouble(parts[22]));
	                	
	                	it2Ana.put((int) extItAnaInfo.getIteration(), extItAnaInfo);
	                }
	                lineCounter++;
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
	        
	        this.runNr2itNr2ana.put(runNr, it2Ana);
			log.info("Loading run outputFile # " + runNr + ": " + runOutputFile.toString() + "... Done.");
	}

	public Map<Integer, Map<Integer, ExtItAnaInfo>> getRunNr2itNr2ana() {
		return runNr2itNr2ana;
	}
	
}
