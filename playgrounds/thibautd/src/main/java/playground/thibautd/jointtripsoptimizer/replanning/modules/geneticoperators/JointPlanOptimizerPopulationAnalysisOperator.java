/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanOptimizerPopulationAnalysisOperator.java
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
package playground.thibautd.jointtripsoptimizer.replanning.modules.geneticoperators;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import org.jgap.GeneticOperator;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;

/**
 * "fake" genetic operator for displaying information about the population.
 * CAUTION: generates large amounts of files at the root of the project
 * @author thibautd
 */
public class JointPlanOptimizerPopulationAnalysisOperator implements GeneticOperator {
	private static final Logger log =
		Logger.getLogger(JointPlanOptimizerPopulationAnalysisOperator.class);


	private static final long serialVersionUID = 1L;

	static int count = 0;
	private final int maxIters;
	private final JointPlanOptimizerJGAPConfiguration jgapConfig;
	private final String fileNameBox;
	private final String fileNameLine;
	private final int populationSize;
	private final int chromosomeLength;
	private int width = 1024;
	private int height = 768;



	//private final List<BoxAndWhiskerItem> boxes;
	private final DefaultBoxAndWhiskerCategoryDataset boxes;
	private final double[][] maxFitnesses;

	public JointPlanOptimizerPopulationAnalysisOperator(
			JointPlanOptimizerJGAPConfiguration jgapConfig,
			int maxIters,
			String outputPath) {
		this.jgapConfig = jgapConfig;
		this.populationSize = jgapConfig.getPopulationSize();
		this.chromosomeLength = jgapConfig.getChromosomeSize();
		this.maxIters = maxIters;
		//this.boxes = new ArrayList<BoxAndWhiskerItem>(maxIters);
		int currentCount = count++;
		this.boxes = new DefaultBoxAndWhiskerCategoryDataset();
		this.maxFitnesses = new double[2][maxIters];

		for (int i=0; i<maxIters; i++) {
			this.maxFitnesses[0][i] = Double.NaN;
			this.maxFitnesses[1][i] = Double.NaN;
		}

		fileNameBox = outputPath+"/fitnessBoxPlot-"+currentCount+".png";
		fileNameLine = outputPath+"/maxFitnessPlot-"+currentCount+".png";
	}

	@Override
	public void operate(
			final Population a_population,
			final List a_candidateChromosome
			) {
		List<Double> fitnesses = new ArrayList<Double>(this.populationSize);
		String seriesName = "fitness: population="+this.populationSize+
			", chomosome size="+a_population.getChromosome(0).size();
		int iterNumber = this.jgapConfig.getGenerationNr() + 1;

		for (int i = 0; i < this.populationSize; i++) {
			fitnesses.add(a_population.getChromosome(i).getFitnessValue());
		}

		this.boxes.add(
				BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(fitnesses),
				seriesName,
				iterNumber);

		this.maxFitnesses[0][iterNumber - 1] = iterNumber;
		this.maxFitnesses[1][iterNumber - 1] = a_population.determineFittestChromosome().getFitnessValue();
	}

	/**
	 * responsible of writing the graphics to file.
	 * Allows the output to work with any number of genetic iterations (ie with a
	 * monitor).
	 */
	@Override
	public void finalize() throws Throwable {
		super.finalize();
		outputFitnessBoxPlots();
		outputBestFitnessGraph();
	}

	private void outputBestFitnessGraph() {
		String title = "best fitness evolution";
		String xLabel = "iteration";
		String yLabel = "fitness";
		boolean legend = false;

		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries(0, this.maxFitnesses);
		JFreeChart chart = ChartFactory.createXYLineChart(
				title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, legend, false, false);

		try {
			ChartUtilities.saveChartAsPNG(new File(fileNameLine), chart, width, height);
		} catch (IOException e) {
			log.warn("problem while writing output graph");
		}
	}

	private void outputFitnessBoxPlots() {
		//log.info("writing fitness chart to file...");

		String title = "fitness: population="+this.populationSize+
			", chomosome size="+this.chromosomeLength;
		String xLabel = "iteration";
		String yLabel = "fitness";
		boolean legend = false;

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				title, xLabel, yLabel, this.boxes, legend);

		try {
			ChartUtilities.saveChartAsPNG(new File(fileNameBox), chart, width, height);
		} catch (IOException e) {
			log.warn("problem while writing output graph");
		}
		//log.info("writing fitness chart to file... DONE");
	}
}

