/* *********************************************************************** *
 * project: org.matsim.*
 * CrossOverRateCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import playground.thibautd.jointtripsoptimizer.run.config.JointReplanningConfigGroup;

/**
 * Updates rate of the cross-overs dynamically, based on the fitnesses of the
 * children in the previous iteration.
 *
 * @author thibautd
 */
public class CrossOverRateCalculator implements RateCalculator {
	private static final Logger log =
		Logger.getLogger(CrossOverRateCalculator.class);

	private static long outputCount = 0L;

	private static int N_OPERATORS = 3;
	private final double[] rates;
	private final double[] contributions;
	private final int[] operationCounts;

	private final double totalRate;

	private final boolean outputEvolution = true;
	private final List<List<Double>> rateMemory;
	private String outputPath;

	/*
	 * =========================================================================
	 * Constructors
	 * =========================================================================
	 */
	public CrossOverRateCalculator(
			final JointReplanningConfigGroup configGroup,
			final String outputPath) {
		this(configGroup.getWholeCrossOverProbability(),
				configGroup.getSimpleCrossOverProbability(),
				configGroup.getSingleCrossOverProbability(),
				outputPath);
	}

	public CrossOverRateCalculator(
			final double wholeCOInitialRate,
			final double simpleCOInitialRate,
			final double singleCOInitialRate,
			final String outputPath) {
		this.rates = new double[N_OPERATORS];
		this.rates[0] = wholeCOInitialRate;
		this.rates[1] = simpleCOInitialRate;
		this.rates[2] = singleCOInitialRate;

		this.contributions = new double[N_OPERATORS];
		this.operationCounts = new int[N_OPERATORS];
		totalRate = wholeCOInitialRate + simpleCOInitialRate + 
			singleCOInitialRate;

		if (outputEvolution) {
			this.rateMemory = new ArrayList<List<Double>>(N_OPERATORS);
			for (int i=0; i < N_OPERATORS; i++) {
				this.rateMemory.add(new ArrayList<Double>());
			}
		}
		else {
			this.rateMemory = null;
		}
		this.outputPath = outputPath;
		this.initializeOperationCounts();
	}

	private void initializeOperationCounts() {
		for (int i=0; i < N_OPERATORS; i++) {
			this.operationCounts[i] = 0;
		}
	}

	/**
	 * @see RateCalculator#getRates()
	 */
	public double[] getRates() {
		return this.rates;
	}

	/**
	 * @see RateCalculator#addResult(int,double,double)
	 */
	public void addResult(
			final int operatorIndex,
			final double fitnessParent,
			final double fitnessChild) {
		double contribution = fitnessChild - fitnessParent;
		this.contributions[operatorIndex] += Math.max(0d, contribution);
		this.operationCounts[operatorIndex]++;
	}

	/**
	 * @see RateCalculator#iterationIsOver()
	 */
	public void iterationIsOver() {
		if (this.outputEvolution) {
			rememberPreviousRates();
		}
		double coef = 0d;

		for (int i=0; i < N_OPERATORS; i++) {
			this.contributions[i] /= this.operationCounts[i];
			coef += this.contributions[i];
		}

		for (int i=0; i < N_OPERATORS; i++) {
			this.rates[i] = (this.contributions[i] / coef) * this.totalRate;
		}
		this.initializeOperationCounts();
	}

	private void rememberPreviousRates() {
		for (int i=0; i < N_OPERATORS; i++) {
			this.rateMemory.get(i).add(this.rates[i]);
		}
	}

	@Override
	public void finalize() throws Throwable {
		super.finalize();
		if (this.outputEvolution) {
			String fileName = this.outputPath+"/CO-rate-evolution-"+(outputCount++)+".png";
			String title = "CO rates evolution";
			String xLabel = "iteration";
			String yLabel = "rate";
			boolean legend = true;
			double[][] currentData;
			List<Double> currentMemory;
			int width = 800;
			int height = 500;

			DefaultXYDataset dataset = new DefaultXYDataset();

			for (int i=0; i < N_OPERATORS; i++) {
				currentMemory = this.rateMemory.get(i);
				currentData = new double[2][currentMemory.size()];
				log.debug("currentMemory: "+currentMemory);
				for (int j=0; j < currentMemory.size(); j++) {
					currentData[0][j] = j;
					currentData[1][j] = currentMemory.get(j);
				}
				dataset.addSeries(i, currentData);
			}
			JFreeChart chart = ChartFactory.createXYLineChart(
					title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, legend, false, false);

			try {
				ChartUtilities.saveChartAsPNG(new File(fileName), chart, width, height);
			} catch (IOException e) {
				log.warn("problem while writing output graph");
			}
		}
	}
}

