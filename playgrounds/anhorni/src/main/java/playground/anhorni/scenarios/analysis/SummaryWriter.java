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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SummaryWriter {
	private String path = "src/main/java/playground/anhorni/";
	private BufferedWriter bufferedWriter = null;	
	private int numberOfRandomRuns = -1;
	private int numberOfCityShoppingLocs = -1;
	
	private double expenditures[][];
	private double sum[];
		
	public SummaryWriter(int numberOfCityShoppingLocs, String outpath, int numberOfRandomRuns) {
		this.numberOfCityShoppingLocs = numberOfCityShoppingLocs;
		this.numberOfRandomRuns = numberOfRandomRuns;
		this.path = outpath;
		this.init();
	}

    private void init() { 
    	
    	this.expenditures = new double[numberOfCityShoppingLocs][numberOfRandomRuns];
    	this.sum = new double[numberOfRandomRuns]; 
    	
    	try {
    		new File(path + "output/PLOC/3towns/random/").mkdir();
            bufferedWriter = new BufferedWriter(new FileWriter(path + "output/PLOC/3towns/random_summary_cityShopping.txt"));           
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } 
        try {
			bufferedWriter.write("config\t");
			for (int i = 0; i < numberOfCityShoppingLocs; i++) {
	        	bufferedWriter.write("loc_" + i + "\t");
	        }
	        bufferedWriter.write("sum\t");
	        bufferedWriter.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void finish() {
    	try {
    		double mean[] = new double [numberOfCityShoppingLocs];
    		double diffSumSquare[] = new double [numberOfCityShoppingLocs];
    		double sigma[] = new double [numberOfCityShoppingLocs];
    		double sumExpenditure[] = new double [numberOfCityShoppingLocs];
    		    		   
    		for (int j = 0; j < numberOfCityShoppingLocs; j++) {
	    		for (int i = 0; i < numberOfRandomRuns; i++) {
	    			sumExpenditure[j] += expenditures[j][i];
	    		}
	    		mean[j] = sumExpenditure[j] / numberOfRandomRuns;
    		}
    		for (int j = 0; j < numberOfCityShoppingLocs; j++) {
	    		for (int i = 0; i < numberOfRandomRuns; i++) {
	    			diffSumSquare[j] += Math.pow((expenditures[j][i] - mean[j]), 2.0);
	    		}
	    		sigma[j] = Math.sqrt(diffSumSquare[j] / (numberOfRandomRuns - 1));
    		}
    		
    		double meanAllRuns = 0;
    		for (int i = 0; i < this.numberOfRandomRuns; i++) {
    			meanAllRuns += this.sum[i];
    		}
    		meanAllRuns /= this.numberOfRandomRuns;
    		
    		double diffSumSquareAll = 0;
    		for (int i = 0; i < this.numberOfRandomRuns; i++) {
    			diffSumSquareAll += Math.pow((this.sum[i] - meanAllRuns), 2.0);
    		}
    		double sigmaAllRuns = Math.sqrt(diffSumSquareAll / numberOfRandomRuns);
    		
    		// output --------------------------------:
    		
    		
    		bufferedWriter.newLine();
    		bufferedWriter.append("Mean:\t");
    		for (int j = 0; j < numberOfCityShoppingLocs; j++) {
    			bufferedWriter.append(mean[j] + "\t");
    		}
    		bufferedWriter.append(String.valueOf(meanAllRuns));
    		bufferedWriter.newLine();
    		
    		bufferedWriter.append("Sigma:\t");
    		for (int j = 0; j < numberOfCityShoppingLocs; j++) {
    			bufferedWriter.append(sigma[j] + "\t");
    		}
    		bufferedWriter.append(String.valueOf(sigmaAllRuns));
    		bufferedWriter.newLine();
    		bufferedWriter.append("Sigma [%]\t");
    		for (int j = 0; j < numberOfCityShoppingLocs; j++) {
    			bufferedWriter.append(sigma[j] / mean[j] * 100 + "\t");
    		}
    		bufferedWriter.append(String.valueOf(sigmaAllRuns / meanAllRuns * 100));
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
         
    public void write2Summary(int runIndex) {
    	
    	double expen[] = new double[numberOfCityShoppingLocs];
    	double sumPerRun = 0;
    	
        try {
          BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "output/PLOC/3towns/random/" + runIndex + "_random_cityShopping.txt"));
          String line = bufferedReader.readLine(); // skip header
          line = bufferedReader.readLine();
          String parts[] = line.split("\t");
          for (int i = 0; i < numberOfCityShoppingLocs; i++) {
        	  expen[i] = Double.parseDouble(parts[i]);
        	  this.expenditures[i][runIndex] = expen[i];
          }          
        } // end try
        catch (IOException e) {
        	e.printStackTrace();
        }

    	try {  
    		bufferedWriter.append("config_" + runIndex + "\t");
    		for (int i = 0; i < numberOfCityShoppingLocs; i++) {
    			bufferedWriter.append(expen[i] + "\t");
    			sumPerRun += expen[i];
    		}
    		sum[runIndex] = sumPerRun;
    		bufferedWriter.append(String.valueOf(sumPerRun));
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }     	
    }
}
