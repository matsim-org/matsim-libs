/* *********************************************************************** *
 * project: org.matsim.*
 * LCControler.java
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

package playground.anhorni.PLOC.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;

import org.apache.log4j.Logger;

public class SummaryWriter {
	private final static Logger log = Logger.getLogger(SummaryWriter.class);
	private String path = "src/main/java/playground/anhorni/";
	private BufferedWriter bufferedWriter = null;	
	private int numberOfRandomRuns;
	private int shoppingFacilities[] = {1, 2, 5, 6, 7};
	
	private double avgExpendituresPerFacilityPerHour[][];
	private double sigmaExpendituresPerFacilityPerHour[][];
	
	private Vector<Run> runs = new Vector<Run>();
			
	public SummaryWriter(String outpath, int numberOfRandomRuns) {
		this.numberOfRandomRuns = numberOfRandomRuns;
		this.path = outpath;
	}
   
    public void run() {
    	this.readRD();
    	this.calculateMeans();
    	this.calculateStdDevOfExpenditures();
    	this.write2Summary();
    	
    	log.info("Create single ensemble analyses");
    	RunsEnsemble runsEnsemble = new RunsEnsemble(0, path + "output/PLOC/3towns/", shoppingFacilities.length);
    	for (Run run : this.runs) {
    		runsEnsemble.addRandomRun(run);
    	}
    	try {
			runsEnsemble.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Create multiple ensemble analyses");
		MultipleEnsemblesAnalyzer analyzer = new MultipleEnsemblesAnalyzer(path, this.runs);
		analyzer.run();		
    }
    
    private void readRD() {
		try {
			for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
				Run run = new Run(runIndex, shoppingFacilities.length);
				for (int day = 0; day < 5; day++) {
					BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "output/PLOC/3towns/run" + runIndex + "/day" + day + "/shoppingPerRunDay.txt"));
					String line = bufferedReader.readLine(); // skip header
					for (int hour = 0; hour < 24; hour++) {
						line = bufferedReader.readLine();
						String parts[] = line.split("\t");
						for (int facIndex = 0; facIndex < shoppingFacilities.length; facIndex++) {
							run.addExpenditure(facIndex, day, hour, Double.parseDouble(parts[facIndex + 1]));
						}
					}
				}
				this.runs.add(run);
			}		
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void calculateMeans() {	
    	this.avgExpendituresPerFacilityPerHour = new double[shoppingFacilities.length][24];
		for (int hour = 0; hour < 24; hour++) {
		    for (int facIndex = 0; facIndex < shoppingFacilities.length; facIndex++) {
		    		for (Run run: this.runs) {
		    			avgExpendituresPerFacilityPerHour[facIndex][hour] += run.getAverageHourlyExpenditures(facIndex, hour) /
		    			(this.numberOfRandomRuns);
		    		}
		    }       
		}
    }
     
    private void calculateStdDevOfExpenditures() {
    	this.sigmaExpendituresPerFacilityPerHour = new double[shoppingFacilities.length][24];
    	for (int hour = 0; hour < 24; hour++) {
		    for (int facIndex = 0; facIndex < shoppingFacilities.length; facIndex++) {
		    	for (int day = 0; day < 5; day++) {
		    		for (Run run: this.runs) {
		    			sigmaExpendituresPerFacilityPerHour[facIndex][hour] += Math.sqrt(
		    					Math.pow(
		    							run.getExpenditure(facIndex, day, hour)
		    							- avgExpendituresPerFacilityPerHour[facIndex][hour], 2.0
		    							)
		    							/ 
		    							(this.numberOfRandomRuns * 5)
		    																	);
		    		}
		    	}
		    }       
		}
    }
         
    public void write2Summary() {
    	DecimalFormat formatter = new DecimalFormat("0.00");
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(path + "output/PLOC/3towns/AllRunsExpendituresPerHourPerFacility.txt")); 
			bufferedWriter.write("Hour\t");
			for (int i = 0; i < shoppingFacilities.length; i++) {
				bufferedWriter.append("f" + shoppingFacilities[i] + "_avg" + "\t" +
						"f" + shoppingFacilities[i] + "_sigma" + "\t" +
						"f" + shoppingFacilities[i] + "_sigma[%]" + 
								"\t");
			}
			bufferedWriter.newLine();
			
			for (int h = 0; h < 24; h++) {
				bufferedWriter.write(h + "\t");
				for (int i = 0; i < shoppingFacilities.length; i++) {
					bufferedWriter.write(formatter.format(avgExpendituresPerFacilityPerHour[i][h]) +"\t");
					bufferedWriter.write(formatter.format(sigmaExpendituresPerFacilityPerHour[i][h]) + "\t");
					bufferedWriter.write(formatter.format(100.0* sigmaExpendituresPerFacilityPerHour[i][h] / avgExpendituresPerFacilityPerHour[i][h]) + "\t");
				}
				bufferedWriter.newLine();
			}
		    bufferedWriter.flush();
		    bufferedWriter.close();
		} 
		catch (IOException e) {
			e.printStackTrace();
			}
    }    
}
