/* *********************************************************************** *
 * project: org.matsim.*
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author ikaddoura
 *
 */
public class MatrixWriter extends BasicWriter{
	private static final Logger log = Logger.getLogger(MatrixWriter.class);

	private List<Integer> numberOfBuses = new ArrayList<Integer>();
    private List<Double> fares = new ArrayList<Double>();
	private BufferedWriter bw;
	
	public MatrixWriter(String outputFolder, Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana) {
		super(outputFolder, runNr2itNr2ana);
		loadParameterData();
	}
	
	public void loadParameterData() {
		Map<Integer, ExtItAnaInfo> firstRunItNr2ana = super.getRunNr2itNr2ana().get(0);
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
	
	public void writeGlobalMaximumMatrix(String type) {
	    Map<Integer, MaximumData> runNr2globalMaximumData = new HashMap<Integer, MaximumData>();

		for (Integer runNr : super.getRunNr2itNr2ana().keySet()){
			
	    	// find global max. point for this run
			Map<Integer, ExtItAnaInfo> itNr2ana = super.getRunNr2itNr2ana().get(runNr);
			double maxFare = 0.;
			int maxBusNumber = 0;
			double maximum = Double.NEGATIVE_INFINITY;
	    	for (Integer it : itNr2ana.keySet()){
	    		if (type.equalsIgnoreCase("Welfare")){
	    			if (itNr2ana.get(it).getWelfare() > maximum){
		    			maximum = itNr2ana.get(it).getWelfare();
		    			maxFare = itNr2ana.get(it).getFare();
		    			maxBusNumber = itNr2ana.get(it).getNumberOfBuses();
		    		} else if (itNr2ana.get(it).getWelfare() == maximum) {
		    			throw new RuntimeException("Exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
		    		}
	    		} else if (type.equalsIgnoreCase("OperatorProfit")){
	    			if (itNr2ana.get(it).getOperatorProfit() > maximum){
		    			maximum = itNr2ana.get(it).getOperatorProfit();
		    			maxFare = itNr2ana.get(it).getFare();
		    			maxBusNumber = itNr2ana.get(it).getNumberOfBuses();
		    		} else if (itNr2ana.get(it).getOperatorProfit() == maximum) {
		    			throw new RuntimeException("Exactly the same operator profit found in two iterations. Don't know what to do. Aborting...");
		    		}
	    		} else {
	    			log.warn(type + " unknown");
	    		}
	    			
			}
	    	MaximumData maxData = new MaximumData();
	    	maxData.setRunNr(runNr);
	    	maxData.setMaximumValue(maximum);
	    	maxData.setMaxFare(maxFare);
	    	maxData.setMaxNumberOfBuses(maxBusNumber);
	    
	    	runNr2globalMaximumData.put(runNr, maxData);
	    }
		
		log.info("Writing analysis output...");
		File file = new File(super.getOutputFolder() + "globalMaximum_" + type + ".csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));

	    for (Double d : this.fares){
	    	String fare = String.valueOf(-1 * d);
	    	bw.write(";" + fare);
	    }
	    
	    bw.newLine();
	    for (Integer nrOfBuses : this.numberOfBuses){
	    	bw.write(nrOfBuses.toString());
	    	
		    Map<Double, Integer> fare2frequency = getFare2frequency(nrOfBuses, runNr2globalMaximumData);
		    for (Double fare : this.fares){
		    	bw.write(";" + fare2frequency.get(fare).toString());
		    }
	    	bw.newLine();
	    }
	    	    
	    bw.flush();
	    bw.close();
	    log.info("Textfile written to " + file.toString());
    
	    } catch (IOException e) {}	
	}

	private Map<Double, Integer> getFare2frequency(int numberOfBuses, Map<Integer, MaximumData> runNr2globalMaximumData) {
		Map<Double, Integer> fare2frequency = new HashMap<Double, Integer>();
		for (Double fare : this.fares){
			int frequencyThisFare = 0;
			for (MaximumData maxWelfareData : runNr2globalMaximumData.values()){
				if (maxWelfareData.getMaxFare() == fare && maxWelfareData.getMaxNumberOfBuses() == numberOfBuses){
					frequencyThisFare++;
				}
			}
			fare2frequency.put(fare, frequencyThisFare);
		}
		return fare2frequency;
	}


	public void writeNumberOfBuses2optimalFareFrequency() {
		Map<Integer, List<Double>> numberOfBuses2optFare = new HashMap<Integer, List<Double>>();
		
    	// find welfare maximizing fare for each number of buses
		
		for (Integer nrOfBuses : this.numberOfBuses){
			List<Double> optFares = null;
			for (Integer runNr : super.getRunNr2itNr2ana().keySet()){
				if (numberOfBuses2optFare.get(nrOfBuses) == null){
					optFares = new ArrayList<Double>();
				} else {
					optFares = numberOfBuses2optFare.get(nrOfBuses);
				}
				Map<Integer, ExtItAnaInfo> itNr2ana = super.getRunNr2itNr2ana().get(runNr);
				double maxWelfareFare = 0.;
				double maxWelfare = Double.NEGATIVE_INFINITY;
		    	for (Integer it : itNr2ana.keySet()){
		    		if (itNr2ana.get(it).getWelfare() > maxWelfare && itNr2ana.get(it).getNumberOfBuses() == nrOfBuses){
		    			maxWelfare = itNr2ana.get(it).getWelfare();
		    			maxWelfareFare = itNr2ana.get(it).getFare();
		    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare && itNr2ana.get(it).getNumberOfBuses() == nrOfBuses) {
		    			throw new RuntimeException("Exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
		    		}	
				}
		    	optFares.add(maxWelfareFare);
		    	numberOfBuses2optFare.put(nrOfBuses, optFares);
		    }
		}
		
		log.info("Writing analysis output...");
		File file = new File(super.getOutputFolder() + "busNumber2welfareMaxFare.csv");
		   
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
		    	
		    	for (Double welfareMaxFare : numberOfBuses2optFare.get(nrOfBuses)){
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

	public void writeFare2optimalNumberOfBusesFrequency() {
		Map<Double, List<Integer>> fare2optBusNr = new HashMap<Double, List<Integer>>();
		// find welfare maximizing bus number for each fare
		
		for (Double fare : this.fares){
			List<Integer> optBusNr = null;
			for (Integer runNr : super.getRunNr2itNr2ana().keySet()){
				if (fare2optBusNr.get(fare) == null){
					optBusNr = new ArrayList<Integer>();
				} else {
					optBusNr = fare2optBusNr.get(fare);
				}
				Map<Integer, ExtItAnaInfo> itNr2ana = super.getRunNr2itNr2ana().get(runNr);
				int maxWelfareBusNr = 0;
				double maxWelfare = Double.NEGATIVE_INFINITY;
		    	for (Integer it : itNr2ana.keySet()){
		    		if (itNr2ana.get(it).getWelfare() > maxWelfare && itNr2ana.get(it).getFare() == fare){
		    			maxWelfare = itNr2ana.get(it).getWelfare();
		    			maxWelfareBusNr = itNr2ana.get(it).getNumberOfBuses();
		    		} else if (itNr2ana.get(it).getWelfare() == maxWelfare && itNr2ana.get(it).getFare() == fare) {
		    			throw new RuntimeException("Exactly the same welfare found in two iterations. Don't know what to do. Aborting...");
		    		}	
				}
		    	optBusNr.add(maxWelfareBusNr);
		    	fare2optBusNr.put(fare, optBusNr);
		    }
		}
		
		log.info("Writing analysis output...");
		File file = new File(super.getOutputFolder() + "fare2welfareMaxBusNumber.csv");
		   
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
		    	
		    	for (Integer welfareMaxBusNr : fare2optBusNr.get(d)){
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

	public void writeAvgMatrix(String type) {
		log.info("Writing analysis output...");
		File file = new File(super.getOutputFolder() + "matrix_avg" + type + ".csv");
		   
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
		    	for (Integer runNr : super.getRunNr2itNr2ana().keySet()){
			    	
			    	Map<Integer, ExtItAnaInfo> itNr2ana = super.getRunNr2itNr2ana().get(runNr);
			    	// for each external iteration (headway-fare combination)
			    	for (Integer it : itNr2ana.keySet()){
			    		
			    		ExtItAnaInfo extItAna = itNr2ana.get(it);
			    		if (extItAna.getNumberOfBuses() == nrOfBuses && extItAna.getFare() == fare){

			    			// extIt of this headway - fare combination
			    			
			    			double data = 0.;
			    			
			    			if (type.equalsIgnoreCase("Welfare")) {
				    			data = extItAna.getWelfare();
			    			} else if (type.equalsIgnoreCase("UserBenefits")) {
			    				data = extItAna.getUsersLogSum();
			    			} else if (type.equalsIgnoreCase("PtTrips")) {
			    				data = extItAna.getNumberOfPtLegs();
			    			} else if (type.equalsIgnoreCase("CarTrips")) {
			    				data = extItAna.getNumberOfCarLegs();
			    			} else if (type.equalsIgnoreCase("OperatorProfit")) {
			    				data = extItAna.getOperatorProfit();
			    			} else if (type.equalsIgnoreCase("MissedBusTrips")) {
			    				data = extItAna.getNumberOfMissedBusTrips();
			    			} else if (type.equalsIgnoreCase("AvgT0minusTActDivByT0perCarTrip")) {
			    				data = extItAna.getAvgT0MinusTActDivByT0perTrip();
			    			} else {
			    				log.warn(type + "unknown.");
			    			}
			    			
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
