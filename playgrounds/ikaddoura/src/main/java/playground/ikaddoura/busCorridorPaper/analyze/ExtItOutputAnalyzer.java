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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author Ihab
 *
 */
public class ExtItOutputAnalyzer {
	private static final Logger log = Logger.getLogger(ExtItOutputAnalyzer.class);
	private Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana = new HashMap<Integer, Map <Integer, ExtItAnaInfo>>();
	private Map<Integer, MaxWelfareData> runNr2maxWelfareData = new HashMap<Integer, MaxWelfareData>();
	private List<Integer> numberOfBuses = new ArrayList<Integer>();
    private List<Double> fares = new ArrayList<Double>();
	private String runFolder;
	private BufferedWriter bw;

	public ExtItOutputAnalyzer(String runFolder) {
		this.runFolder = runFolder;
	}

	public void loadData() {
		
		log.info("Loading run outputs from folder " + this.runFolder);
		File folder = new File(this.runFolder);
		int runNr = 0;
		for ( File file : folder.listFiles() ) {
			if (file.isDirectory() && !file.isHidden()){
				String runOutputFile = file.toString() + "/extItData.txt";
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
	                	String[] parts = line.split(" ; "); 
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
	                	extItAnaInfo.setNoValidPlanScore(Double.parseDouble(parts[19]));
	                	
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

	public void writeWelfareData(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "welfare.txt");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION ; NumberOfBuses ; Fare (AUD) ";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write("; Welfare RunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(" ; AVG ; MIN ; MAX ");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + " ; " + firstRunItNr2ana.get(iteration).getNumberOfBuses() + " ; " + firstRunItNr2ana.get(iteration).getFare());
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		double welfare = this.runNr2itNr2ana.get(runNr).get(iteration).getWelfare();
		   		memValues.add(welfare);
		   		bw.write(" ; " + String.valueOf(welfare));
		   	}
		   	bw.write(" ; " + getAverage(memValues).toString());
		   	bw.write(" ; " + getMin(memValues).toString());
		   	bw.write(" ; " + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}

	
	private Double getMin(List<Double> memValues) {
		double min = Double.POSITIVE_INFINITY;
		for (Double value : memValues){
			if (value < min) {
				min = value;
			}
		}

		return min;
	}
	
	private Double getMax(List<Double> memValues) {
		double max = Double.NEGATIVE_INFINITY;
		for (Double value : memValues){
			if (value > max) {
				max = value;
			}
		}
		return max;
	}

	private Double getAverage(List<Double> memValues) {
		int n = 0;
		double sum = 0;
		for (Double value : memValues){
			n++;
			sum = sum + value;
		}
		double avg = sum/n;
		return avg;
	}

	public void loadWelfareDensityData() {
		for (Integer runNr : this.runNr2itNr2ana.keySet()){
			
	    	// find optimal point for this run
			Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			double maxWelfareFare = 0.;
			int maxWelfareNumberOfBuses = 0;
			double minWelfareFare = 0.;
			int minWelfareNumberOfBuses = 0;
			double maxWelfare = Double.NEGATIVE_INFINITY;
			double minWelfare = Double.POSITIVE_INFINITY;
	    	for (Integer it : itNr2ana.keySet()){
	    		if (itNr2ana.get(it).getWelfare() > maxWelfare){
	    			maxWelfare = itNr2ana.get(it).getWelfare();
	    			maxWelfareFare = itNr2ana.get(it).getFare();
	    			maxWelfareNumberOfBuses = itNr2ana.get(it).getNumberOfBuses();
	    		} else if (itNr2ana.get(it).getWelfare() < minWelfare){
	    			minWelfare = itNr2ana.get(it).getWelfare();
	    			minWelfareFare = itNr2ana.get(it).getFare();
	    			minWelfareNumberOfBuses = itNr2ana.get(it).getNumberOfBuses();
	    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare) {
	    			throw new RuntimeException("Oups, exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
	    		}	
			}
	    	MaxWelfareData maxWelfareData = new MaxWelfareData();
	    	maxWelfareData.setRunNr(runNr);
	    	maxWelfareData.setMaxWelfare(maxWelfare);
	    	maxWelfareData.setMinWelfare(minWelfare);
	    	maxWelfareData.setMaxWelfareFare(maxWelfareFare);
	    	maxWelfareData.setMinWelfareFare(minWelfareFare);
	    	maxWelfareData.setMaxWelfareNumberOfBuses(maxWelfareNumberOfBuses);
	    	maxWelfareData.setMinWelfareNumberOfBuses(minWelfareNumberOfBuses);
	    
	    	this.runNr2maxWelfareData.put(runNr, maxWelfareData);
	    	
	    } 
	}

	public void writeDensityData(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "maxWelfareDensity.txt");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
	    	int nrOfBuses = firstRunItNr2ana.get(iteration).getNumberOfBuses();
	    	double fare = firstRunItNr2ana.get(iteration).getFare();
	    	if (!this.numberOfBuses.contains(nrOfBuses)){
	    		this.numberOfBuses.add(nrOfBuses);
	    	}
	    	
	    	if (!this.fares.contains(fare)){
	    		this.fares.add(fare);
	    	}
	    }

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(" ; " + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    Map<Double, Integer> fare2frequency = getFare2frequency(nrOfBuses);
		    for (Double fare : this.fares){
		    	bw.write(" ; " + fare2frequency.get(fare).toString());
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}				
	}

	private Map<Double, Integer> getFare2frequency(int numberOfBuses) {
		Map<Double, Integer> fare2frequency = new HashMap<Double, Integer>();
		for (Double fare : this.fares){
			int frequencyThisFare = 0;
			for (MaxWelfareData maxWelfareData : this.runNr2maxWelfareData.values()){
				if (maxWelfareData.getMaxWelfareFare() == fare && maxWelfareData.getMaxWelfareNumberOfBuses() == numberOfBuses){
					frequencyThisFare++;
				}
			}
			System.out.println(fare + " . " + frequencyThisFare);
			fare2frequency.put(fare, frequencyThisFare);
		}
		return fare2frequency;
	}
	
}
