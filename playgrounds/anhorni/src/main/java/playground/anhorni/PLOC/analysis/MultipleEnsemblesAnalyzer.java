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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;

import playground.anhorni.PLOC.ConfigReader;

public class MultipleEnsemblesAnalyzer {
	private final static Logger log = Logger.getLogger(MultipleEnsemblesAnalyzer.class);
	private int numberOfEnsembles = 1;
	private  Random rnd = new Random(109876L);
	private ConfigReader myConfigReader = null;
	private String outpath;
	
	private int shoppingFacilities[] = {1, 2, 5, 6, 7};
	private Vector<Run> runs;
			
	public MultipleEnsemblesAnalyzer(String outpath, Vector<Run> runs) {
		this.outpath = outpath;
		this.runs = runs;
		this.init();
	}
	
	private void init() {
		myConfigReader = new ConfigReader();
		myConfigReader.read();	
		this.numberOfEnsembles = this.myConfigReader.getNumberOfAnalyses();
	}
	
	 public void run() {
		 for (int hour = 12; hour < 20; hour++) {
			 TreeMap<Integer, Vector<RunsEnsemble>> runsEnsemblesPerSize = new TreeMap<Integer, Vector<RunsEnsemble>>();
			 for (int ensembleSize = 0; ensembleSize < runs.size(); ensembleSize++) {
				 Vector<RunsEnsemble> runsEnsembles = new Vector<RunsEnsemble>();
				 for (int i = 0; i < numberOfEnsembles; i++) {
					 Collections.shuffle(runs, rnd);
					 RunsEnsemble runsEnsemble = new RunsEnsemble(0, "", shoppingFacilities.length);
					 for (int k = 0; k <= ensembleSize; k++) {
						 runsEnsemble.addRandomRun(runs.get(k));
					 }
					 runsEnsembles.add(runsEnsemble);
				 }
				 runsEnsemblesPerSize.put(ensembleSize, runsEnsembles);
			 }
			 log.info("Writting multiple ensembles hour = " + hour);
			 this.printHourlyAnalysis(runsEnsemblesPerSize, hour);
		 }
	}
	 
	private void printHourlyAnalysis(TreeMap<Integer, Vector<RunsEnsemble>> runsEnsemblesPerSize, int hour) {
		
		for (int facIndex = 0; facIndex < shoppingFacilities.length; facIndex++) {
			MultipleEnsemblesBoxPlot boxPlot = new MultipleEnsemblesBoxPlot("Facility " + shoppingFacilities[facIndex] + 
					" hour " + hour + ": " + numberOfEnsembles + " Ensembles");
			for (int i = 0; i < runsEnsemblesPerSize.values().size(); i++) {
				ArrayList<Double> averageExpenditures = new ArrayList<Double>();
				for (RunsEnsemble runsEnsemble : runsEnsemblesPerSize.get(i)) {
					averageExpenditures.add(runsEnsemble.getMean(facIndex, hour));
					boxPlot.addSeriesPerEnsembleSize(averageExpenditures, i + 1);
				}
			}
			boxPlot.createChart();
			boxPlot.saveAsPng(this.outpath + "/output/PLOC/3towns/facility" + shoppingFacilities[facIndex] + 
					"/multipleEnsembles_facility" + shoppingFacilities[facIndex] + "_hour" + hour + ".png", 1000, 500);
		}
	}	
	
        
//    
//    private void createBunchOfAnalysis(double refVal) { 
//    	DecimalFormat formatter = new DecimalFormat("0.00");
//		BufferedWriter bufferedWriter = null;
//		
//		double x[] = new double[this.numberOfRandomRuns];
//		for (int i = 0; i < this.numberOfRandomRuns; i++) {
//			x[i] = i + 1;
//		}
//		
//		try {
//		
//		for (int locIndex = 0; locIndex < this.numberOfCityShoppingLocs; locIndex++) {
//			String dir = this.path + "/output/PLOC/3towns/loc_" + locIndex + "/";
//			XYScatterChart chart = new XYScatterChart("loc_" + locIndex, "Number of sample runs used to build average", "Deviation from Mean_n [%]");
//			
//			bufferedWriter = new BufferedWriter(new FileWriter(dir +  "summary.txt"));			
//			new File(dir).mkdir();
//			
//			bufferedWriter.write("Analysis:\t");
//			for (int i = 0; i < this.numberOfRandomRuns; i++) {
//				bufferedWriter.append(i + "\t");
//			}
//			bufferedWriter.newLine();
//						
//			int analysisIndex = 0;
//			for (RunsEnsemble analysis : this.analyses) {
//				bufferedWriter.append(analysisIndex + "\t");
//				double[] singleLocMeanExpenditures = analysis.computeSingleLocationAnalysisPercentage(locIndex, refVal);					
//				chart.addSeries("", x, singleLocMeanExpenditures);
//
//				for (int i = 0; i < singleLocMeanExpenditures.length; i++) {
//						bufferedWriter.append(String.valueOf(formatter.format(singleLocMeanExpenditures[i])) + "\t");		
//				}
//				analysisIndex++;
//				bufferedWriter.newLine();
//				bufferedWriter.flush();
//			}
//			bufferedWriter.close();
//			chart.saveAsPng(dir + "summary.png" , 1000, 500);
//		}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//    }
//    
//    private void createEnsembleAnalysis(double refValue) {
//    	for (int locIndex = 0; locIndex < this.numberOfCityShoppingLocs; locIndex++)  {
//    		
//    		TreeMap<Integer, Vector<Double>> valuesPerNumberOfSamples = new TreeMap<Integer, Vector<Double>>();
//    		for (int i = 0; i < this.numberOfRandomRuns; i++) {
//    			valuesPerNumberOfSamples.put(i, new Vector<Double>());
//    		}
//    		for (RunsEnsemble analysis : this.analyses) {
//    			double [] meanValuesAnalysis = analysis.computeSingleLocationAnalysisPercentage(locIndex, refValue);
//    			for (int i = 0; i < this.numberOfRandomRuns; i++) {
//    				valuesPerNumberOfSamples.get(i).add(meanValuesAnalysis[i]);
//    			}
//    	   	}   		
//    		EnsembleAnalysisBoxPlot plot = new EnsembleAnalysisBoxPlot(valuesPerNumberOfSamples, "Ensemble Analysis",
//    				this.numberOfRandomRuns, locIndex, myConfigReader.getRunId());
//    		String superVal = "_rel";
//    		if (refValue > 0.0) {
//    			superVal = "_abs";
//    		}
//    		
//    		plot.saveAsPng("src/main/java/playground/anhorni/output/PLOC/3towns/random/loc_" + locIndex + "/" + myConfigReader.getRunId() + superVal + ".png", 1500, 700);  		
//    	}	
//    }
}
