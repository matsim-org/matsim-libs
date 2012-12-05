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
	private Map<Integer, MaxWelfareData> runNr2globalMaxWelfareData = new HashMap<Integer, MaxWelfareData>();
	private Map<Integer, List<Double>> numberOfBuses2optFare = new HashMap<Integer, List<Double>>();
	private Map<Double, List<Integer>> fare2optBusNr = new HashMap<Double, List<Integer>>();

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

	public void writeWelfare(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "welfare.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write("; Welfare RunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		double welfare = this.runNr2itNr2ana.get(runNr).get(iteration).getWelfare();
		   		memValues.add(welfare);
		   		bw.write(";" + String.valueOf(welfare));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writePtTrips(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "ptTrips.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";PtTripsRunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getNumberOfPtLegs();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}

	public void writeCarTrips(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "carTrips.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";CarTripsRunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getNumberOfCarLegs();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writeMissedBusTrips(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "missedBusTrips.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";MissedBusTrips_RunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getNumberOfMissedBusTrips();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writeAvgT0minusTActDivByT0perCarTrip(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "avgT0minusTActDivByT0perCarTrip.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";avgT0minusTActDivByT0perCarTrip_RunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getAvgT0MinusTActDivByT0perTrip();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writeOperatorProfit(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "operatorProfit.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";OperatorProfitRunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getOperatorProfit();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
	        bw.newLine();
	    }

	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}		
	}
	
	public void writeUsersLogSum(String outputPath) {
		log.info("Writing analysis output...");
		File file = new File(outputPath + "userBenefits.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr : this.runNr2itNr2ana.keySet()){
	    	bw.write(";UserBenefitsRunNr_" + runNr.toString());
	    	if (this.runNr2itNr2ana.get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    
	    // ..........
	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana = this.runNr2itNr2ana.get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr : this.runNr2itNr2ana.keySet()){
		   		
		    	double data = this.runNr2itNr2ana.get(runNr).get(iteration).getUsersLogSum();
		   		memValues.add(data);
		   		
		   		bw.write(";" + String.valueOf(data));
		   	}
		   	bw.write(";" + getAverage(memValues).toString());
		   	bw.write(";" + getMin(memValues).toString());
		   	bw.write(";" + getMax(memValues).toString());
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

	public void writeGlobalMaxWelfareMatrix(String outputPath) {
		for (Integer runNr : this.runNr2itNr2ana.keySet()){
			
	    	// find global max. point for this run
			Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			double maxWelfareFare = 0.;
			int maxWelfareNumberOfBuses = 0;
			double maxWelfare = Double.NEGATIVE_INFINITY;
	    	for (Integer it : itNr2ana.keySet()){
	    		if (itNr2ana.get(it).getWelfare() > maxWelfare){
	    			maxWelfare = itNr2ana.get(it).getWelfare();
	    			maxWelfareFare = itNr2ana.get(it).getFare();
	    			maxWelfareNumberOfBuses = itNr2ana.get(it).getNumberOfBuses();
	    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare) {
	    			throw new RuntimeException("Oups, exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
	    		}	
			}
	    	MaxWelfareData maxWelfareData = new MaxWelfareData();
	    	maxWelfareData.setRunNr(runNr);
	    	maxWelfareData.setMaxWelfare(maxWelfare);
	    	maxWelfareData.setMaxWelfareFare(maxWelfareFare);
	    	maxWelfareData.setMaxWelfareNumberOfBuses(maxWelfareNumberOfBuses);
	    
	    	this.runNr2globalMaxWelfareData.put(runNr, maxWelfareData);
	    }
		
		log.info("Writing analysis output...");
		File file = new File(outputPath + "runNr2globalMaxWelfareMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    Map<Double, Integer> fare2frequency = getFare2frequency(nrOfBuses);
		    for (Double fare : this.fares){
		    	bw.write(";" + fare2frequency.get(fare).toString());
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
			for (MaxWelfareData maxWelfareData : this.runNr2globalMaxWelfareData.values()){
				if (maxWelfareData.getMaxWelfareFare() == fare && maxWelfareData.getMaxWelfareNumberOfBuses() == numberOfBuses){
					frequencyThisFare++;
				}
			}
			fare2frequency.put(fare, frequencyThisFare);
		}
		return fare2frequency;
	}

	public void loadParameterData() {
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
	}

	public void writeNumberOfBuses2optimalFareFrequency(String outputPath) {
		
    	// find welfare maximizing fare for each number of buses
		
		for (Integer nrOfBuses : this.numberOfBuses){
			List<Double> optFares = null;
			for (Integer runNr : this.runNr2itNr2ana.keySet()){
				if (numberOfBuses2optFare.get(nrOfBuses) == null){
					optFares = new ArrayList<Double>();
				} else {
					optFares = numberOfBuses2optFare.get(nrOfBuses);
				}
				Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
				double maxWelfareFare = 0.;
				double maxWelfare = Double.NEGATIVE_INFINITY;
		    	for (Integer it : itNr2ana.keySet()){
		    		if (itNr2ana.get(it).getWelfare() > maxWelfare && itNr2ana.get(it).getNumberOfBuses() == nrOfBuses){
		    			maxWelfare = itNr2ana.get(it).getWelfare();
		    			maxWelfareFare = itNr2ana.get(it).getFare();
		    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare && itNr2ana.get(it).getNumberOfBuses() == nrOfBuses) {
		    			throw new RuntimeException("Oups, exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
		    		}	
				}
		    	optFares.add(maxWelfareFare);
		    	numberOfBuses2optFare.put(nrOfBuses, optFares);
		    }
		}
		
		log.info("Writing analysis output...");
		File file = new File(outputPath + "busNumber2welfareMaxFare.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    
	    bw.write("for each headway --> frequency of welfare maximum fare");
	    bw.newLine();
	    
	    bw.write("Number of buses");
	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());

		    for (Double fare : this.fares){
		    	int fareCounter = 0;
		    	
		    	for (Double welfareMaxFare : this.numberOfBuses2optFare.get(nrOfBuses)){
		    		if (welfareMaxFare.doubleValue() == fare.doubleValue()) {
		    			fareCounter++;
		    		}
		    	}
		    	bw.write(";" + fareCounter);
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}	
	}

	public void writeFare2optimalNumberOfBusesFrequency(String outputPath) {
		// find welfare maximizing bus number for each fare
		
		for (Double fare : this.fares){
			List<Integer> optBusNr = null;
			for (Integer runNr : this.runNr2itNr2ana.keySet()){
				if (this.fare2optBusNr.get(fare) == null){
					optBusNr = new ArrayList<Integer>();
				} else {
					optBusNr = this.fare2optBusNr.get(fare);
				}
				Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
				int maxWelfareBusNr = 0;
				double maxWelfare = Double.NEGATIVE_INFINITY;
		    	for (Integer it : itNr2ana.keySet()){
		    		if (itNr2ana.get(it).getWelfare() > maxWelfare && itNr2ana.get(it).getFare() == fare){
		    			maxWelfare = itNr2ana.get(it).getWelfare();
		    			maxWelfareBusNr = itNr2ana.get(it).getNumberOfBuses();
		    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare && itNr2ana.get(it).getFare() == fare) {
		    			throw new RuntimeException("Oups, exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
		    		}	
				}
		    	optBusNr.add(maxWelfareBusNr);
		    	fare2optBusNr.put(fare, optBusNr);
		    }
		}
		
		log.info("Writing analysis output...");
		File file = new File(outputPath + "fare2welfareMaxBusNumber.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    bw.write("for each fare --> frequency of welfare maximum number of buses");
	    bw.newLine();

	    bw.write("Fare (AUD)");
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(";" + nrOfBuses);
	    }
	    
	    bw.newLine();
	    
	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(fare.toString());

		    for (Integer busNr : this.numberOfBuses){
		    	int busNrCounter = 0;
		    	
		    	for (Integer welfareMaxBusNr : this.fare2optBusNr.get(d)){
		    		if (welfareMaxBusNr.doubleValue() == busNr.doubleValue()) {
		    			busNrCounter++;
		    		}
		    	}
		    	bw.write(";" + busNrCounter);
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}	
	}

	public void writeAvgWelfareMatrix(String outputPath) {
		
		log.info("Writing analysis output...");
		File file = new File(outputPath + "avgWelfareMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average welfare for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double welfare = extItAna.getWelfare();
			    			memValues.add(welfare);
			    		}
			    	}
			   	}
			   	double avgWelfare = getAverage(memValues);
			   	
		    	// done - average welfare calculated
		    	
		    	bw.write(";" + String.valueOf(avgWelfare));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	    
	}


	public void writeAvgPtTripsMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgPtTripsMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getNumberOfPtLegs();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void writeAvgCarTripsMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgCarTripsMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getNumberOfCarLegs();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void writeAvgAvgT0minusTActDivByT0perCarTripMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgAvgT0minusTActDivByT0perCarTripMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getAvgT0MinusTActDivByT0perTrip();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void writeAvgMissedBusTripsMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgMissedBusTripsMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getNumberOfMissedBusTrips();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void writeAvgOperatorProfitMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgOperatorProfitMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getOperatorProfit();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}

	public void writeAvgUsersLogSumMatrix(String outputFolder) {
		log.info("Writing analysis output...");
		File file = new File(outputFolder + "avgUsersLogSumMatrix.csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    for (Double fare : this.fares){
		    	
		    	// calculate average data for this headway - fare combination
		    	
		    	List<Double> memValues = new ArrayList<Double>();

		    	// for each random Seed run
		    	for (Integer runNr : this.runNr2itNr2ana.keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = this.runNr2itNr2ana.get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			double data = extItAna.getUsersLogSum();
			    			memValues.add(data);
			    		}
			    	}
			   	}
			   	double avgData = getAverage(memValues);
			   	
		    	// done - average data calculated
		    	
		    	bw.write(";" + String.valueOf(avgData));
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to "+file.toString());
    
	    } catch (IOException e) {}
	}
	
}
