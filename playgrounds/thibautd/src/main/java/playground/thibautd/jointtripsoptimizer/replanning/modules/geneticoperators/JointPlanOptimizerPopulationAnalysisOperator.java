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

import java.awt.Color;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;

import org.jgap.Gene;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.impl.BooleanGene;
import org.jgap.Population;

import playground.thibautd.jointtripsoptimizer.replanning.modules.JointPlanOptimizerJGAPConfiguration;

/**
 * "fake" genetic operator for displaying information about the population.
 * CAUTION: generates large amounts of files. Use with little scenarios only
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
	private final String fileNameNiche;
	private final int populationSize;
	private final int chromosomeLength;
	private final int nMembers;
	private double scale = 0.75d;
	private int width = (int) (scale * 1024); 
	private int height = (int) (scale *  768);



	//private final List<BoxAndWhiskerItem> boxes;
	private final DefaultBoxAndWhiskerCategoryDataset boxes;
	private final double[][] maxFitnesses;
	private final double[][] nNiches;

	public JointPlanOptimizerPopulationAnalysisOperator(
			JointPlanOptimizerJGAPConfiguration jgapConfig,
			int maxIters,
			int nMembers,
			String outputPath) {
		this.jgapConfig = jgapConfig;
		this.nMembers = nMembers;
		this.populationSize = jgapConfig.getPopulationSize();
		this.chromosomeLength = jgapConfig.getChromosomeSize();
		this.maxIters = maxIters;
		//this.boxes = new ArrayList<BoxAndWhiskerItem>(maxIters);
		int currentCount = count++;
		this.boxes = new DefaultBoxAndWhiskerCategoryDataset();
		this.maxFitnesses = new double[2][maxIters];
		this.nNiches = new double[2][maxIters];

		for (int i=0; i<maxIters; i++) {
			this.maxFitnesses[0][i] = Double.NaN;
			this.maxFitnesses[1][i] = Double.NaN;
			this.nNiches[0][i] = Double.NaN;
			this.nNiches[1][i] = Double.NaN;

		}

		fileNameBox = outputPath+"/fitnessBoxPlot-"+nMembers+"-members-size-"+chromosomeLength+"-"+currentCount+".png";
		fileNameLine = outputPath+"/maxFitnessPlot-"+nMembers+"-members-size-"+chromosomeLength+"-"+currentCount+".png";
		fileNameNiche = outputPath+"/nichePlot-"+nMembers+"-members-size-"+chromosomeLength+"-"+currentCount+".png";
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

		this.nNiches[0][iterNumber - 1] = iterNumber;
		this.nNiches[1][iterNumber - 1] = countNiches(a_population);
	}

	private int countNiches(final Population population) {
		int count = 0;
		List<Boolean> scheme;
		List<List<Boolean>> knownSchemes = new ArrayList<List<Boolean>>();

		for (Object chrom : population.getChromosomes()) {
			scheme = new ArrayList<Boolean>();
			for (Gene gene : ((IChromosome) chrom).getGenes()) {
				if (gene instanceof BooleanGene) {
					scheme.add(((BooleanGene) gene).booleanValue());
				}
			}

			if (!knownSchemes.contains(scheme)) {
				knownSchemes.add(scheme);
				count++;
			}
		}

		return count;
	}

	/**
	 * responsible of writing the graphics to file.
	 * Allows the output to work with any number of genetic iterations (ie with a
	 * monitor).
	 */
	public void finish() {
		outputFitnessBoxPlots();
		outputBestFitnessGraph();
		outputNichesGraph();
	}

	private void outputBestFitnessGraph() {
		String title = "best fitness evolution, "+nMembers+" agents";
		String xLabel = "iteration";
		String yLabel = "fitness";
		boolean legend = false;

		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries(0, this.maxFitnesses);
		JFreeChart chart = ChartFactory.createXYLineChart(
				title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, legend, false, false);
		// set the X axis use integer values.
		NumberAxis axis = new NumberAxis();
		axis.setTickUnit((NumberTickUnit) NumberAxis.createIntegerTickUnits().getCeilingTickUnit(1d));
		axis.setAutoRangeIncludesZero(false);
		(chart.getXYPlot()).setDomainAxis(axis);
		chart.getPlot().setBackgroundPaint(Color.white);
		try {
			ChartUtilities.saveChartAsPNG(new File(fileNameLine), chart, width, height);
		} catch (IOException e) {
			log.warn("problem while writing output graph");
		}
	}

	private void outputFitnessBoxPlots() {
		//log.info("writing fitness chart to file...");

		String title = "fitness: population="+this.populationSize+
			", chomosome size="+this.chromosomeLength+", "+
			this.nMembers+" agents";
		String xLabel = "iteration";
		String yLabel = "fitness";
		boolean legend = false;

		JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
				title, xLabel, yLabel, this.boxes, legend);

		chart.getPlot().setBackgroundPaint(Color.white);
		try {
			ChartUtilities.saveChartAsPNG(new File(fileNameBox), chart, width, height);
		} catch (IOException e) {
			log.warn("problem while writing output graph");
		}
		//log.info("writing fitness chart to file... DONE");
	}

	private void outputNichesGraph() {
		String title = "number of collaboration schemes: population="+this.populationSize+
			", chomosome size="+this.chromosomeLength+", "+
			this.nMembers+" agents";
		String xLabel = "iteration";
		String yLabel = "n";
		boolean legend = false;

		DefaultXYDataset dataset = new DefaultXYDataset();
		dataset.addSeries(0, this.nNiches);
		JFreeChart chart = ChartFactory.createXYLineChart(
				title, xLabel, yLabel, dataset, PlotOrientation.VERTICAL, legend, false, false);
		// set the X axis use integer values.
		NumberAxis axis = new NumberAxis();
		axis.setTickUnit((NumberTickUnit) NumberAxis.createIntegerTickUnits().getCeilingTickUnit(1d));
		axis.setAutoRangeIncludesZero(false);
		(chart.getXYPlot()).setDomainAxis(axis);
		chart.getPlot().setBackgroundPaint(Color.white);
		try {
			ChartUtilities.saveChartAsPNG(new File(fileNameNiche), chart, width, height);
		} catch (IOException e) {
			log.warn("problem while writing output graph");
		}
	}
}

