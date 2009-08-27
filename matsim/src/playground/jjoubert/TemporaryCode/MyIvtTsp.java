/* *********************************************************************** *
 * project: org.matsim.*
 * MyIvtTsp.java
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

package playground.jjoubert.TemporaryCode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import playground.jjoubert.RetailerGA.MyFitnessFunction;
import playground.jjoubert.RetailerGA.RetailerGA;
import playground.jjoubert.Utilities.DateString;
import playground.jjoubert.Utilities.MyPermutator;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

import com.vividsolutions.jts.geom.Coordinate;

public class MyIvtTsp {

	private final static Logger log = Logger.getLogger(MyIvtTsp.class);
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int genomeLength = 20;
		int populationSize = 50;
		int numberOfGenerations = 1000;
		double elites = 0.10;
		double mutants = 0.2;
		int crossoverType = 3;

		MyFitnessFunction mff = new MyFitnessFunction(false, genomeLength);
		ArrayList<Coordinate> points = mff.getPoints();
		DenseDoubleMatrix2D dist = mff.getDistanceMatrix();

		writeToFile(points, dist);
		MyPermutator mp = new MyPermutator();
		Integer[] someSolution = {1,14,19,4,20,7,15,3,2,11,18,12,16,6,17,13,9,10,8,5};
		ArrayList<Integer> initial = new ArrayList<Integer>(genomeLength);
		for (Integer integer : someSolution) {
			initial.add(integer);
		}
		RetailerGA ga = new RetailerGA(populationSize, genomeLength, mff, mp.permutate(genomeLength));
//		RetailerGA ga = new RetailerGA(populationSize, genomeLength, mff, initial);
		ArrayList<ArrayList<Double>> solutionProgress = new ArrayList<ArrayList<Double>>(numberOfGenerations);		

		for(int i = 0; i < numberOfGenerations; i++){
			ga.evolve(elites, mutants, crossoverType, null);
			solutionProgress.add(ga.getStats());
		}

		/*
		 * Print out the last generation to the console.
		 */
		String out = ga.toString();
		System.out.printf(out);
		System.out.printf("\nStatistics for crossover type %d:\n", crossoverType);
		System.out.printf("\t                   Genome length:  %d\n", genomeLength);
		System.out.printf("\t                 Population size:  %d\n", populationSize);
		System.out.printf("\t           Number of generations:  %d\n", numberOfGenerations);
		System.out.printf("\t               Incumbent fitness:  %6.2f\n", ga.getIncumbent().getFitness());

		/*
		 * Print out the solution progress to a file for R-graph.
		 */
		DateString ds = new DateString();
		String fileName = "/Users/johanwjoubert/R-Source/Input/GA-TransportOpt-" + ds.toString() + ".txt";		
		writeSolutionProgressToFile(solutionProgress, fileName);
	}
	
	private static void writeSolutionProgressToFile(
			ArrayList<ArrayList<Double>> solutionProgress,
			String fileName) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(fileName)));
			try{
				output.write("Iteration,Best,Average,Worst");
				output.newLine();
				int iteration = 0;
				for (ArrayList<Double> solution : solutionProgress) {
					output.write(String.valueOf(iteration));
					output.write(",");
					output.write(String.valueOf(solution.get(0)));
					output.write(",");
					output.write(String.valueOf(solution.get(1)));
					output.write(",");
					output.write(String.valueOf(solution.get(2)));
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


	private static void writeToFile(ArrayList<Coordinate> points, DenseDoubleMatrix2D dist) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("/Users/johanwjoubert/R-Source/Input/TspCoordinates.txt")));
			try{
				bw.write("X\tY");
				bw.newLine();
				for (Coordinate coordinate : points) {
					bw.write(String.valueOf(coordinate.x));
					bw.write("\t");
					bw.write(String.valueOf(coordinate.y));
					bw.newLine();
				}
			} finally{
				bw.close();
			}

			bw = new BufferedWriter(new FileWriter(new File("/Users/johanwjoubert/R-Source/Input/TspDistance.txt")));
			try{
				bw.write("    ");
				for(int i = 0; i < 20; i++){
					bw.write("");
					bw.write(String.format("%4d", (i+1)));
					bw.write("  ");
				}
				bw.newLine();
				for(int row = 0; row < dist.rows(); row++){
					bw.write(String.format("%2d", (row+1)));
					bw.write("  ");
					for(int col = 0; col < dist.columns(); col++){
						bw.write(String.format("%4d",Math.round(dist.getQuick(row, col))));
						bw.write("  ");
					}
					bw.newLine();
				}
			} finally{
				bw.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
