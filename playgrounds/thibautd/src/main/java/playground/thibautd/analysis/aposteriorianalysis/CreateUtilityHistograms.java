/* *********************************************************************** *
 * project: org.matsim.*
 * CreateUtilityHistograms.java
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
package playground.thibautd.analysis.aposteriorianalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;
import playground.thibautd.utils.WrapperChartUtil;
import playground.thibautd.utils.XYLineHistogramDataset;

/**
 * @author thibautd
 */
public class CreateUtilityHistograms {
	private static final Log log =
		LogFactory.getLog(CreateUtilityHistograms.class);

	private static final double BIN_WIDTH = 5;
	// size of the cliques to analyse (negative means all cliques)
	private static final int N_MEMBERS = 2;

	/**
	 * true for getting data from dumps of a previously analysed population
	 */
	private static final boolean GET_FROM_EXPORT = false;

	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;

	public static void main(final String[] args){
		XYLineHistogramDataset dataset;
		String outputPath;

		if (GET_FROM_EXPORT) {
			String globalFile = args[0];
			String jointFile = args[1];
			String passengerFile = args[2];
			String individualFile = args[3];
			outputPath = args[4];

			dataset = getHistogramDataset(globalFile, jointFile, passengerFile, individualFile);
		}
		else {
			String configFile1 = args[0];
			String configFile2 = args[1];
			outputPath = args[2];

			ScenarioWithCliques scenarioIndividual = JointControlerUtils.createScenario(configFile1);
			ScenarioWithCliques scenarioJoint = JointControlerUtils.createScenario(configFile2);

			dataset = getHistogramDataset(scenarioJoint, scenarioIndividual, outputPath);
		}

		//line
		ChartUtil globalHistogram = getLineHistogram(dataset);

		globalHistogram.saveAsPng(outputPath+"/globalHistogram.png", WIDTH, HEIGHT);
		int seriesCount = globalHistogram.getChart().getXYPlot().getDataset().getSeriesCount();

		// plot each histogram individually
		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}

		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, true);
			globalHistogram.saveAsPng(outputPath+"/globalHistogram-"+i+".png", WIDTH, HEIGHT);
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}

		//cumul
		globalHistogram = getCumulative(dataset);
		globalHistogram.saveAsPng(outputPath+"/globalHistogram-cumul.png", WIDTH, HEIGHT);

		// plot each histogram individually
		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}

		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, true);
			globalHistogram.saveAsPng(outputPath+"/globalHistogram-cumul-"+i+".png", WIDTH, HEIGHT);
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}

		//plain
		globalHistogram = getPlainHistogram(dataset);
		globalHistogram.saveAsPng(outputPath+"/globalHistogram-plain.png", WIDTH, HEIGHT);

		// plot each histogram individually
		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}

		for (int i = 0; i < seriesCount; i++) {
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, true);
			globalHistogram.saveAsPng(outputPath+"/globalHistogram-plain-"+i+".png", WIDTH, HEIGHT);
			globalHistogram.getChart().getXYPlot().getRenderer().setSeriesVisible(i, false);
		}
	}

	private static void tuneDomainAxis(final ValueAxis axis) {
		axis.setLowerBound(0);
		axis.setUpperBound(800);
	}

	private static ChartUtil getLineHistogram(
			final XYLineHistogramDataset dataset) {
		dataset.setCumulative(false);
		JFreeChart chart = ChartFactory.createXYLineChart(
				"distribution of scores",
				"score",
				"frequency",
				dataset, 
				PlotOrientation.VERTICAL,
				true,
				false,
				false);

		tuneDomainAxis(chart.getXYPlot().getDomainAxis());
		return new WrapperChartUtil(chart);
	}

	private static ChartUtil getCumulative(
			final XYLineHistogramDataset dataset) {
		dataset.setCumulative(true);
		JFreeChart chart = ChartFactory.createXYLineChart(
				"cumulative distribution of scores",
				"score",
				"frequency",
				dataset, 
				PlotOrientation.VERTICAL,
				true,
				false,
				false);
		tuneDomainAxis(chart.getXYPlot().getDomainAxis());

		return new WrapperChartUtil(chart);
	}

	private static ChartUtil getPlainHistogram(
			final XYLineHistogramDataset dataset) {
		dataset.setCumulative(false);
		JFreeChart chart = ChartFactory.createHistogram(
				"distribution of scores",
				"score",
				"frequency",
				dataset, 
				PlotOrientation.VERTICAL,
				true,
				false,
				false);
		tuneDomainAxis(chart.getXYPlot().getDomainAxis());

		return new WrapperChartUtil(chart);
	}



	private static XYLineHistogramDataset getHistogramDataset(
			final ScenarioWithCliques scenario,
			final ScenarioWithCliques scenarioIndividual,
			final String outputPath) {
		List<Double> globalScores = new ArrayList<Double>();
		List<Double> jointScores = new ArrayList<Double>();
		List<Double> passengerScores = new ArrayList<Double>();
		List<Double> individualScores = new ArrayList<Double>();
		//String globalString = "";
		//String jointString = "";
		//String passengerString = "";
		
		try {
			log.info("begin dataset construction");
			Plan currentPlan;
			double score;
			double indivScore;

			BufferedWriter globalWriter = IOUtils.getBufferedWriter(outputPath+"/globalScores.txt");
			BufferedWriter jointWriter = IOUtils.getBufferedWriter(outputPath+"/jointScores.txt");
			BufferedWriter passengerWriter = IOUtils.getBufferedWriter(outputPath+"/passengerScores.txt");
			BufferedWriter individualWriter = IOUtils.getBufferedWriter(outputPath+"/individual-popJoint.txt");
			Map<Id, ? extends Person> individuals = scenarioIndividual.getPopulation().getPersons();

			for (Clique clique : scenario.getCliques().getCliques().values()) {
				if (N_MEMBERS > 0 && clique.getMembers().size() != N_MEMBERS) continue;
				for (Person person : clique.getMembers().values()) {
					currentPlan = person.getSelectedPlan();
					score = currentPlan.getScore();

					globalScores.add(score);
					globalWriter.write(""+score);
					globalWriter.newLine();

					if (isJoint(currentPlan)) {
						jointScores.add(score);
						jointWriter.write(""+score);
						jointWriter.newLine();

						indivScore = individuals.get(person.getId()).getSelectedPlan().getScore();
						individualScores.add(indivScore);
						individualWriter.write(""+indivScore);
						individualWriter.newLine();

						if (isPassenger(currentPlan)) {
							passengerScores.add(score);
							passengerWriter.write(""+score);
							passengerWriter.newLine();
						}
					}
				}
			}

			log.info("writing data files");
			globalWriter.close();
			jointWriter.close();
			passengerWriter.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("returning dataset");
		return getHistogramDataset(globalScores, jointScores, passengerScores, individualScores);
	}

	private static XYLineHistogramDataset getHistogramDataset(
			final String globalFile,
			final String jointFile,
			final String passengerFile,
			final String individualFile) {
		List<Double> globalScores = new ArrayList<Double>();
		List<Double> jointScores = new ArrayList<Double>();
		List<Double> passengerScores = new ArrayList<Double>();
		List<Double> individualScores = new ArrayList<Double>();

		try {
			BufferedReader reader = IOUtils.getBufferedReader(globalFile);
			String value = reader.readLine();

			while (value != null) {
				globalScores.add(Double.parseDouble(value));
				value = reader.readLine();
			}

			reader = IOUtils.getBufferedReader(jointFile);
			value = reader.readLine();
			while (value != null) {
				jointScores.add(Double.parseDouble(value));
				value = reader.readLine();
			}

			reader = IOUtils.getBufferedReader(passengerFile);
			value = reader.readLine();
			while (value != null) {
				passengerScores.add(Double.parseDouble(value));
				value = reader.readLine();
			}

			reader = IOUtils.getBufferedReader(individualFile);
			value = reader.readLine();
			while (value != null) {
				individualScores.add(Double.parseDouble(value));
				value = reader.readLine();
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return getHistogramDataset(globalScores, jointScores, passengerScores, individualScores);
	}

	private static XYLineHistogramDataset getHistogramDataset(
			final List<Double> globalScores,
			final List<Double> jointScores,
			final List<Double> passengerScores,
			final List<Double> individualScores) {
		XYLineHistogramDataset dataset = new XYLineHistogramDataset(BIN_WIDTH);
		log.info("maximum global score: "+Collections.max(globalScores));
		log.info("maximum joint score: "+Collections.max(jointScores));
		log.info("maximum passenger score: "+Collections.max(passengerScores));
		log.info("maximum individual score: "+Collections.max(individualScores));

		dataset.addSeries(
				"All agents ("+globalScores.size()+" agents)",
				globalScores);
		dataset.addSeries(
				"Agents with joint trips ("+jointScores.size()+" agents)",
				jointScores);
		dataset.addSeries(
				"Agents with passenger trips ("+passengerScores.size()+" agents)",
				passengerScores);
		dataset.addSeries(
				"Agents with joint trips, when optimised without joint trips ("+individualScores.size()+" agents)",
				individualScores);

		return dataset;
	}

	/**
	 * @return true if the plan contains at least one joint trip
	 */
	private static boolean isJoint(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity && ((Activity) pe).getType().equals(JointActingTypes.DROP_OFF)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @return true if the plan contains at least one passenger trip
	 */
	private static boolean isPassenger(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg && ((Leg) pe).getMode().equals(JointActingTypes.PASSENGER)) {
				return true;
			}
		}

		return false;
	}
}

