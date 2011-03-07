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

package playground.anhorni.scenarios.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SummaryWriter {
	private String path = "src/main/java/playground/anhorni/";
	private BufferedWriter bufferedWriter = null;	
	private int numberOfRandomRuns;
	private int shoppingFacilities[] = {1, 2, 5, 6, 7};
	
	private double expendituresPerRunPerDayPerHourPerFacility[][][][];
	private double avgExpendituresPerHourPerFacility[][];
	private double sigmaExpendituresPerHourPerFacility[][];
			
	public SummaryWriter(String outpath, int numberOfRandomRuns) {
		this.numberOfRandomRuns = numberOfRandomRuns;
		this.path = outpath;
	}
   
    public void run() {
    	this.readRD();
    	this.calculateMeans();
    	this.calculateStdDevOfExpenditures();
    	this.write2Summary();
    }
    
    private void calculateStdDevOfExpenditures() {
    	this.sigmaExpendituresPerHourPerFacility = new double[24][shoppingFacilities.length];
    	for (int h = 0; h < 24; h++) {
		    for (int i = 0; i < shoppingFacilities.length; i++) {
		    	for (int day = 0; day < 5; day++) {
		    		for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
		    			sigmaExpendituresPerHourPerFacility[h][i] += Math.sqrt(
		    					Math.pow(
		    							expendituresPerRunPerDayPerHourPerFacility[runIndex][day][h][i]
		    							- avgExpendituresPerHourPerFacility[h][i], 2.0
		    							)
		    							/ 
		    							(this.numberOfRandomRuns * 5)
		    																	);
		    		}
		    	}
		    }       
		}
    }
    
    private void calculateMeans() {	
    	this.avgExpendituresPerHourPerFacility = new double[24][shoppingFacilities.length];
		for (int h = 0; h < 24; h++) {
		    for (int i = 0; i < shoppingFacilities.length; i++) {
		    	for (int day = 0; day < 5; day++) {
		    		for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
		    			avgExpendituresPerHourPerFacility[h][i] += expendituresPerRunPerDayPerHourPerFacility[runIndex][day][h][i] /
		    			(this.numberOfRandomRuns * 5);
		    		}
		    	}
		    }       
		}
    }
    
    private void readRD() {
    	this.expendituresPerRunPerDayPerHourPerFacility = new double[this.numberOfRandomRuns][5][24][shoppingFacilities.length];
    	try {
        	for (int runIndex = 0; runIndex < numberOfRandomRuns; runIndex++) {
        		for (int day = 0; day < 5; day++) {
					BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "output/PLOC/3towns/R" + runIndex + "D" + day + "_shoppingPerRunDay.txt"));
					String line = bufferedReader.readLine(); // skip header
					for (int h = 0; h < 24; h++) {
					    line = bufferedReader.readLine();
					    String parts[] = line.split("\t");
					    for (int i = 1; i < shoppingFacilities.length; i++)
					    	expendituresPerRunPerDayPerHourPerFacility[runIndex][day][h][i-1] = Double.parseDouble(parts[i]);
						}
					}
        	}        
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }
    }
         
    public void write2Summary() {
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(path + "output/PLOC/3towns/ExpendituresPerHourPerFacility.txt")); 
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
					bufferedWriter.write(avgExpendituresPerHourPerFacility[h][i] +"\t");
					bufferedWriter.write(sigmaExpendituresPerHourPerFacility[h][i] + "\t");
					bufferedWriter.write(100.0* sigmaExpendituresPerHourPerFacility[h][i] / avgExpendituresPerHourPerFacility[h][i] + "\t");
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
