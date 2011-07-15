/* *********************************************************************** *
 * project: org.matsim.*
 * ComparePlanomat.java
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

import java.io.BufferedWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.charts.ChartUtil;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;
import playground.thibautd.utils.WrapperChartUtil;
import playground.thibautd.utils.XYLineHistogramDataset;

/**
 * @author thibautd
 */
public class ComparePlanomat {
	private static final double BIN_WIDTH = 2;

	public static void main(final String[] args) {
		String configFilePlanomat = args[0];
		String configFileJPO = args[1];
		String outputPath = args[2];

		Scenario scenarioPlanomat = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFilePlanomat));
		ScenarioWithCliques scenarioJPO = JointControlerUtils.createScenario(configFileJPO);

		ChartUtil chart = getChart(scenarioPlanomat, scenarioJPO);
		chart.saveAsPng(outputPath+"indiv-planomat_scores.png",800,600);

		XYLineHistogramDataset dataset = getHistogramDataset(scenarioPlanomat, scenarioJPO, outputPath);
		chart = getLineHistogram(dataset);
		chart.saveAsPng(outputPath+"indiv-planomat_histogram.png",800,600);

		chart = getCumulative(dataset);
		chart.saveAsPng(outputPath+"indiv-planomat_cumul.png",800,600);

	}

	private static ChartUtil getChart(
			final Scenario scenarioPlanomat,
			final ScenarioWithCliques scenarioJPO) {
		Map<Id, ? extends Person> planomatIndividuals = scenarioPlanomat.getPopulation().getPersons();
		Map<Integer, Averager> values = new HashMap<Integer, Averager>();
		Averager currentAverager;

		for (Clique clique : scenarioJPO.getCliques().getCliques().values()) {
			currentAverager = values.get(clique.getMembers().size());

			if (currentAverager == null) {
				currentAverager = new Averager();
				values.put(clique.getMembers().size(), currentAverager);
			}

			for (Map.Entry<Id, ? extends Person> entry : clique.getMembers().entrySet()) {
				currentAverager.addJPO(entry.getValue().getSelectedPlan().getScore());
				currentAverager.addPlanomat(planomatIndividuals.get(entry.getKey()).getSelectedPlan().getScore());
			}
		}

		List<Tuple<Integer, Double>> planomatData = new ArrayList<Tuple<Integer, Double>>();
		List<Tuple<Integer, Double>> jpoData = new ArrayList<Tuple<Integer, Double>>();

		for (Map.Entry<Integer, Averager> entry : values.entrySet()) {
			planomatData.add(new Tuple<Integer,Double>(entry.getKey(), entry.getValue().getPlanomatAverage()));
			jpoData.add(new Tuple<Integer,Double>(entry.getKey(), entry.getValue().getJPOAverage()));
		}

		Collections.sort(planomatData, new TupleComparator());
		Collections.sort(jpoData, new TupleComparator());

		double[] x = new double[planomatData.size()];
		double[] yPlanomat = new double[planomatData.size()];
		double[] yJpo = new double[planomatData.size()];

		for (int i=0; i < x.length; i++) {
			x[i] = planomatData.get(i).getFirst();
			yPlanomat[i] = planomatData.get(i).getSecond();
			yJpo[i] = jpoData.get(i).getSecond();
		}

		XYLineChart chart = new XYLineChart(
				"average executed score per clique size, iteration 30",
				"clique size",
				"score");
		chart.addSeries("planomat", x, yPlanomat);
		chart.addSeries("joint plan optimiser with individual trips", x, yJpo);
		chart.getChart().getXYPlot().setRenderer(
				new XYLineAndShapeRenderer(
					true, // draw lines
					true)); // draw points

		return chart;
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

	private static void tuneDomainAxis(final ValueAxis axis) {
		axis.setLowerBound(0);
		axis.setUpperBound(800);
	}



	private static XYLineHistogramDataset getHistogramDataset(
			final Scenario scenarioPlanomat,
			final Scenario scenarioJPO,
			final String outputPath) {
		List<Double> planomatScores = new ArrayList<Double>();
		List<Double> jpoScores = new ArrayList<Double>();
		
		try {
			Plan currentPlan;
			double score;

			BufferedWriter planomatWriter = IOUtils.getBufferedWriter(outputPath+"/planomatscores.txt");
			BufferedWriter jpoWriter = IOUtils.getBufferedWriter(outputPath+"/jposcores.txt");

			for (Person person : scenarioPlanomat.getPopulation().getPersons().values()) {
				currentPlan = person.getSelectedPlan();
				score = currentPlan.getScore();

				planomatScores.add(score);
				planomatWriter.write(""+score);
				planomatWriter.newLine();

			}

			for (Person person : scenarioJPO.getPopulation().getPersons().values()) {
				currentPlan = person.getSelectedPlan();
				score = currentPlan.getScore();

				jpoScores.add(score);
				jpoWriter.write(""+score);
				jpoWriter.newLine();

			}

			planomatWriter.close();
			jpoWriter.close();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return getHistogramDataset(planomatScores, jpoScores);
	}

	private static XYLineHistogramDataset getHistogramDataset(
			final List<Double> planomatScores,
			final List<Double> jpoScores) {
		XYLineHistogramDataset dataset = new XYLineHistogramDataset(BIN_WIDTH);

		dataset.addSeries(
				"planomat",
				planomatScores);
		dataset.addSeries(
				"joint plan optimisation algorithm",
				jpoScores);

		return dataset;
	}

	private static class Averager {
		private final List<Double> planomatValues = new ArrayList<Double>();
		private final List<Double> jpoValues = new ArrayList<Double>();

		public void addPlanomat(final Double value) {
			planomatValues.add(value);
		}

		public void addJPO(final Double value) {
			jpoValues.add(value);
		}

		public double getPlanomatAverage() {
			return getAverage(planomatValues);
		}

		public double getJPOAverage() {
			return getAverage(jpoValues);
		}

		public double getAverage(final List<Double> values) {
			double sum = 0;
			double count = 0;

			for (double x : values) {
				sum += x;
				count += 1;
			}

			return sum / count;
		}
	}

	private static class TupleComparator implements Comparator<Tuple<? extends Comparable, ? extends Object>> {
		@Override
		public int compare(Tuple<? extends Comparable, ? extends Object> t1,
				Tuple<? extends Comparable, ? extends Object> t2) {
			return t1.getFirst().compareTo(t2.getFirst());
		}
	}
}
