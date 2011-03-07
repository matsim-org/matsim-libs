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

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import java.io.BufferedReader;
//import java.io.BufferedWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import org.matsim.core.utils.charts.XYScatterChart;

import playground.anhorni.scenarios.ConfigReader;

public class RandomRunsAnalyzer {
	private String path;
//	private BufferedWriter bufferedWriter = null;	
	private int numberOfRandomRuns = -1;
	private int numberOfCityShoppingLocs = -1;
	private  Random rnd = new Random(109876L);
	private Vector<Analysis> analyses;	
	private double superValuePerLocation = 0;
	private ConfigReader myConfigReader = null;
			
	public RandomRunsAnalyzer(int numberOfCityShoppingLocs, String outpath, int numberOfRandomRuns) {
		this.numberOfCityShoppingLocs = numberOfCityShoppingLocs;
		this.numberOfRandomRuns = numberOfRandomRuns;
		this.path = outpath;
		this.analyses = new Vector<Analysis>();	
		this.init();
	}
	
	private void init() {
		myConfigReader = new ConfigReader();
		myConfigReader.read();		  
	}
	
	 public void run(int numberOfAnalysis) {		 
		 Vector<RandomRun> randomRuns = this.readRuns();
		 	
		 	for (int k = 0; k < numberOfAnalysis; k++) {
		 		Analysis analysis = this.createAnalysis(k, randomRuns);
		 		analysis.write();
		 		this.analyses.add(analysis);
		 	}
		 	this.createEnsembleAnalysis(0.0);
		 	this.createEnsembleAnalysis(this.superValuePerLocation);
		 	this.createBunchOfAnalysis(0.0);
	}
    
    private Vector<RandomRun> readRuns() {	
    	Vector<RandomRun> randomRuns = new Vector<RandomRun>();
    	
		try {
			  BufferedReader bufferedReader = new BufferedReader(new FileReader(path + "output/PLOC/3towns/summaryShopping.txt"));
			  String line = bufferedReader.readLine(); // skip header
			  for (int j = 0; j < this.numberOfRandomRuns; j++) {
				  RandomRun randomRun = new RandomRun(j, numberOfCityShoppingLocs);
			      line = bufferedReader.readLine();
			      String parts[] = line.split("\t");
			      for (int i = 0; i < numberOfCityShoppingLocs; i++) {
			    	  randomRun.addExpenditure(i, Double.parseDouble(parts[i + 1]));
			      } 
			      randomRuns.add(randomRun);
			  }
		} // end try
		    catch (IOException e) {
		    	e.printStackTrace();
		}
		    return randomRuns;
	}
     
    private Analysis createAnalysis(int analysisId, Vector<RandomRun> randomRuns) {
    	Collections.shuffle(randomRuns, this.rnd);
    	
    	Analysis analysis = new Analysis(analysisId, this.path, this.numberOfCityShoppingLocs);    	
    	for (RandomRun rr : randomRuns) {
    		analysis.addRandomRun(rr);
    	}
    	return analysis;
    }
    
    private void createBunchOfAnalysis(double refVal) { 
    	DecimalFormat formatter = new DecimalFormat("0.00");
		BufferedWriter bufferedWriter = null;
		
		double x[] = new double[this.numberOfRandomRuns];
		for (int i = 0; i < this.numberOfRandomRuns; i++) {
			x[i] = i + 1;
		}
		
		try {
		
		for (int locIndex = 0; locIndex < this.numberOfCityShoppingLocs; locIndex++) {
			String dir = this.path + "/output/PLOC/3towns/loc_" + locIndex + "/";
			XYScatterChart chart = new XYScatterChart("loc_" + locIndex, "Number of sample runs used to build average", "Deviation from Mean_n [%]");
			
			bufferedWriter = new BufferedWriter(new FileWriter(dir +  "summary.txt"));			
			new File(dir).mkdir();
			
			bufferedWriter.write("Analysis:\t");
			for (int i = 0; i < this.numberOfRandomRuns; i++) {
				bufferedWriter.append(i + "\t");
			}
			bufferedWriter.newLine();
						
			int analysisIndex = 0;
			for (Analysis analysis : this.analyses) {
				bufferedWriter.append(analysisIndex + "\t");
				double[] singleLocMeanExpenditures = analysis.computeSingleLocationAnalysisPercentage(locIndex, refVal);					
				chart.addSeries("", x, singleLocMeanExpenditures);

				for (int i = 0; i < singleLocMeanExpenditures.length; i++) {
						bufferedWriter.append(String.valueOf(formatter.format(singleLocMeanExpenditures[i])) + "\t");		
				}
				analysisIndex++;
				bufferedWriter.newLine();
				bufferedWriter.flush();
			}
			bufferedWriter.close();
			chart.saveAsPng(dir + "summary.png" , 1000, 500);
		}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private void createEnsembleAnalysis(double refValue) {
    	for (int locIndex = 0; locIndex < this.numberOfCityShoppingLocs; locIndex++)  {
    		
    		TreeMap<Integer, Vector<Double>> valuesPerNumberOfSamples = new TreeMap<Integer, Vector<Double>>();
    		for (int i = 0; i < this.numberOfRandomRuns; i++) {
    			valuesPerNumberOfSamples.put(i, new Vector<Double>());
    		}
    		for (Analysis analysis : this.analyses) {
    			double [] meanValuesAnalysis = analysis.computeSingleLocationAnalysisPercentage(locIndex, refValue);
    			for (int i = 0; i < this.numberOfRandomRuns; i++) {
    				valuesPerNumberOfSamples.get(i).add(meanValuesAnalysis[i]);
    			}
    	   	}   		
    		EnsembleAnalysisBoxPlot plot = new EnsembleAnalysisBoxPlot(valuesPerNumberOfSamples, "Ensemble Analysis",
    				this.numberOfRandomRuns, locIndex, myConfigReader.getRunId());
    		String superVal = "_rel";
    		if (refValue > 0.0) {
    			superVal = "_abs";
    		}
    		
    		plot.saveAsPng("src/main/java/playground/anhorni/output/PLOC/3towns/random/loc_" + locIndex + "/" + myConfigReader.getRunId() + superVal + ".png", 1500, 700);  		
    	}	
    }
}
