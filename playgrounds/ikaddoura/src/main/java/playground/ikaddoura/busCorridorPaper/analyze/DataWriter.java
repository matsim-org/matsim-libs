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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * @author ikaddoura
 *
 */
public class DataWriter extends BasicWriter{
	private static final Logger log = Logger.getLogger(DataWriter.class);
	private BufferedWriter bw;
	
	public DataWriter(String outputFolder, Map<Integer, Map<Integer, ExtItAnaInfo>> runNr2itNr2ana) {
		super(outputFolder, runNr2itNr2ana);
	}

	public void writeData(String type) {
		log.info("Writing analysis output...");
		File file = new File(super.getOutputFolder() + type + ".csv");
		   
	    try {
	    bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "ITERATION;NumberOfBuses;Fare (AUD)";
	    bw.write(zeile1);
	    
	    int iterations = 0;
	    for (Integer runNr :  super.getRunNr2itNr2ana().keySet()){
	    	bw.write(";" + type + "_RunNr_"+ runNr.toString());
	    	if ( super.getRunNr2itNr2ana().get(runNr).size() != iterations && iterations != 0) {
	    		throw new RuntimeException("Different number of iterations in different runs. Aborting...");
	    	}
	    }
	    
	    bw.write(";AVG;MIN;MAX");
	    
	    bw.newLine();
	    	    
    	Map<Integer, ExtItAnaInfo> firstRunItNr2ana =  super.getRunNr2itNr2ana().get(0);
    	for (int iteration = 0; iteration <= firstRunItNr2ana.size()-1; iteration++){
		    bw.write(iteration + ";" + firstRunItNr2ana.get(iteration).getNumberOfBuses() + ";" + firstRunItNr2ana.get(iteration).getFare() * (-1));
		    List<Double> memValues = new ArrayList<Double>();
		    for (Integer runNr :  super.getRunNr2itNr2ana().keySet()){
		   		
		    	double data = 0.;
		    	
		    	if (type.equalsIgnoreCase("Welfare")) {
	    			data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getWelfare();
    			} else if (type.equalsIgnoreCase("UserBenefits")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getUsersLogSum();
    			} else if (type.equalsIgnoreCase("PtTrips")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getNumberOfPtLegs();
    			} else if (type.equalsIgnoreCase("CarTrips")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getNumberOfCarLegs();
    			} else if (type.equalsIgnoreCase("OperatorProfit")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getOperatorProfit();
    			} else if (type.equalsIgnoreCase("MissedBusTrips")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getNumberOfMissedBusTrips();
    			} else if (type.equalsIgnoreCase("AvgT0minusTActDivByT0perCarTrip")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getAvgT0MinusTActDivByT0perTrip();
    			} else if (type.equalsIgnoreCase("AvgWaitingTimeAll")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getAvgWaitingTimeAll();
    			} else if (type.equalsIgnoreCase("AvgWaitingTimeMissedBus")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getAvgWaitingTimeMissing();
    			} else if (type.equalsIgnoreCase("AvgWaitingTimeNoMissedBus")) {
    				data = super.getRunNr2itNr2ana().get(runNr).get(iteration).getAvgWaitingTimeNotMissing();
    			} else {
    				log.warn(type + "unknown.");
    			}
		    	
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

}
