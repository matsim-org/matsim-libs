/* *********************************************************************** *
 * project: org.matsim.*
 * Variance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.yu.analysis;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPlansReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Plans;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithmI;
import org.matsim.utils.io.IOUtils;
import org.matsim.world.World;

/**
 * @author yu
 * 
 */
public class ScoreVariance extends PersonAlgorithm implements PlanAlgorithmI {
	private final List<Double> scores = new ArrayList<Double>();
	private BufferedWriter writer;
	private final String outputFilename;

	public ScoreVariance(final String outputFilename) {
		this.outputFilename = outputFilename;
		try {
			writer = IOUtils.getBufferedWriter(outputFilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run(final Person person) {
		run(person.getSelectedPlan());
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// final String netFilename = "../data/ivtch/input/network.xml";
		final String netFilename = "../data/schweiz/input/ch.xml";
		final String plansFilename = "../data/schweiz/input/Run410.output.plans.xml.gz";
		final String outputFilename = "../data/schweiz/variance/Run410variance.txt";

		Gbl.startMeasurement();
		Gbl.createConfig(null);

		World world = Gbl.getWorld();

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(netFilename);
		world.setNetworkLayer(network);

		Plans population = new Plans();

		ScoreVariance sv = new ScoreVariance(outputFilename);
		population.addAlgorithm(sv);

		System.out.println("-->reading plansfile: " + plansFilename);
		new MatsimPlansReader(population).readFile(plansFilename);

		population.runAlgorithms();
		sv.writeVariance();

		System.out.println("--> Done!");
		Gbl.printElapsedTime();
		System.exit(0);
	}

	public void run(final Plan plan) {
		scores.add(plan.getScore());
	}

	public void writeVariance() {
		int size = scores.size();
		if (size == 0) {
			try {
				writer
						.write(outputFilename
								+ "\nERROR: there is not data for calculating Variance!");
				writer.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(
					"ERROR: there is not data for calculating Variance!");
		}
		double[] scoreArray = new double[size];
		for (int i = 0; i < size; i++)
			scoreArray[i] = scores.get(i);
		double var = getVariance(scoreArray);
		try {
			writer.write(outputFilename + "\ncount = " + scoreArray.length
					+ "\navg. = " + getAverage(scoreArray)
					+ "\nScoreVariance = " + var + "\nStandard deviation = "
					+ getStandardDeviation(var));
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double getStandardDeviation(final double variance) {
		return Math.sqrt(variance);
	}

	public static double getStandardDeviation(final double[] inputData) {
		return Math.sqrt(getVariance(inputData));
	}

	public static double getVariance(final double[] inputData) {
		double average = getAverage(inputData);
		if (average == -1) {
			System.err.println("avg. = " + -1);
			System.exit(0);
		}
		return getSquareSum(inputData) / getCount(inputData) - average
				* average;
	}

	public static int getCount(final double[] inputData) {
		return inputData == null ? -1 : inputData.length;
	}

	public static double getAverage(final double[] inputData) {
		return inputData == null || inputData.length == 0 ? -1
				: getSum(inputData) / inputData.length;
	}

	public static double getSum(final double[] inputData) {
		if (inputData == null || inputData.length == 0)
			return -1;
		double sum = 0;
		for (double element : inputData)
			sum += element;
		return sum;
	}

	public static double getSquareSum(final double[] inputData) {
		if (inputData == null || inputData.length == 0)
			return -1;
		double sqrsum = 0.0;
		for (double element : inputData)
			sqrsum += element * element;
		return sqrsum;
	}
}
