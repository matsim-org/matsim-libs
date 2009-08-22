/* *********************************************************************** *
 * project: org.matsim.*
 * RunRetailerSA.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.RetailerSA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.log4j.Logger;

import playground.jjoubert.Utilities.DateString;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

public class RunRetailerSA {
	
	private final static Logger log = Logger.getLogger(RunRetailerSA.class);
	private static DenseDoubleMatrix2D p  = null;
	private static DenseDoubleMatrix2D d = null;
	private static DenseDoubleMatrix1D D = null;
	private static int numberOfIterations = 500;
	private static double initialTemperature = 2;
	private static int temperatureReductionFrequency = Math.max(5, numberOfIterations/10);
	private static double temperatureReductionFactor = 0.75;

	
	public static void main(String[] args){
		log.info("Initiating the Simulated Annealing (SA) parameter estimation model.");
		
		/* 
		 * Read the data so that you have three matrices (row: person; column: retailer)
		 * 		- p, a DenseDoubleMatrix2D of the observed likelihoods;
		 * 		- d, a DenseDoubleMatrix2D of the distances;
		 * 		- D, a DenseDoubleMatrix1D of the retailer sizes.
		 */
		readCiariSample();
		
		/*
		 * The essence of the Simulated Annealing (SA) algorithm. You need to first create
		 * a fitness function to calculate the likelihood. 
		 */
		MySaFitnessFunction ff = new MySaFitnessFunction(p, d, D);
		/*
		 * I just used an arbitrary initial solution, but you can provide any ArrayList
		 * containing an initial solution. I assumed it will only contain two values.
		 */
		ArrayList<Double> initial = new ArrayList<Double>(2);
		initial.add(0.5);
		initial.add(0.5);
		/*
		 * Create an instance of the RetailerSA. The first four arguments are algorithm
		 * parameters and you can set them above: they're been declared static variables
		 * in this class.
		 */
		RetailerSA rsa = new RetailerSA(initialTemperature, 
										temperatureReductionFrequency, 
										temperatureReductionFactor, 
										numberOfIterations, initial, ff, false);
		long tStart = System.currentTimeMillis();
		rsa.estimateParameters();
		
		/*
		 * Just getting the solution statistics back from the algorithm, and calculating
		 * the duration of the run.
		 */
		long tDuration = System.currentTimeMillis() - tStart;
		ArrayList<Double[]> progress = rsa.getSolutionProgress();
		ArrayList<Double> incumbent = rsa.getIncumbentSolution();
		double incumbentObjective = rsa.getIncumbentObjective();
		
		/*
		 * Reporting.
		 */
		DateString ds = new DateString();
		String outputFilename = "/Users/johanwjoubert/R-Source/Input/SA-Progress-" + ds.toString() + ".txt";
		writeProgressToFile(progress, outputFilename);
		
		writeLogReport(tDuration, incumbent, incumbentObjective);
	}
	

	/**
	 * Just write some reporting to the log-file.
	 */
	private static void writeLogReport(long tDuration,
			ArrayList<Double> incumbent, double incumbentObjective) {
		log.info("Took " + tDuration + "ms for " + numberOfIterations + " iterations.");
		log.info("=============================================================================");
		log.info("Algorithm parameters:");
		log.info("");
		log.info("     Initial temperature: " + initialTemperature);
		log.info("     Temperature reduction frequency: " + temperatureReductionFrequency);
		log.info("     Temperature reduction factor: " + temperatureReductionFactor);
		log.info("     Number of iterations: " + numberOfIterations);
		log.info("=============================================================================");
		log.info("Solution parameters:");
		log.info("");
		log.info("     Beta1: " + incumbent.get(0));
		log.info("     Beta2: " + incumbent.get(1));
		log.info("     Square error: " + incumbentObjective);
		log.info("=============================================================================");
		log.info("Done.");
	}
	
	private static void writeProgressToFile(ArrayList<Double[]> solutionProgress,
			String filename) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename)));
			try{
				output.write("Iteration,Incumbent,Current");
				output.newLine();
				int iteration = 0;
				for (Double[] solution : solutionProgress) {
					output.write(String.valueOf(iteration));
					output.write(",");
					output.write(String.valueOf(solution[0]));
					output.write(",");
					output.write(String.valueOf(solution[1]));
					output.newLine();
					iteration++;
				}				
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void readCiariSample(){
		log.info("Reading sample data from matrices.");
		int shops = 5;
		ArrayList<Double> data1 = new ArrayList<Double>();
		try {
			Scanner input1 = new Scanner(new BufferedReader(new FileReader(new File("/Users/johanwjoubert/Desktop/Temp/Ciari/matrix1.txt"))));
			while(input1.hasNext()){
				Double value = new Double(Double.parseDouble(input1.next()));
				data1.add(value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int people = data1.size() / shops;
		
		log.info("Read " + data1.size() + " values from each matrix. Includes " + shops + " shops and " + people + " people.");
		ArrayList<Double> data2 = new ArrayList<Double>();
		try {
			Scanner input2 = new Scanner(new BufferedReader(new FileReader(new File("/Users/johanwjoubert/Desktop/Temp/Ciari/matrix2.txt"))));
			while(input2.hasNextLine()){
				String[] line = input2.nextLine().split(" ");
				Double value = new Double(Double.parseDouble(line[0]));
				data2.add(value);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		int dataIndex = 0;
		p = new DenseDoubleMatrix2D(people, shops);
		d = new DenseDoubleMatrix2D(people, shops);
		for(int i = 0; i < people; i++){
			for(int j = 0; j < shops; j++){
				p.set(i, j, data1.get(dataIndex));
				d.set(i, j, data2.get(dataIndex));
				dataIndex++;
			}
		}
		D = new DenseDoubleMatrix1D(shops);
		double[] sizes = {91.0,161.0,111.0,221.0,251.0};
		D.assign(sizes);		
	}

}
