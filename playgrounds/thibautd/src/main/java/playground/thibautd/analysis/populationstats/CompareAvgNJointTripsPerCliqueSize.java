/* *********************************************************************** *
 * project: org.matsim.*
 * CompareAvgNJointTripsPerCliqueSize.java
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
package playground.thibautd.analysis.populationstats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.charts.XYLineChart;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.ScenarioWithCliques;
import playground.thibautd.jointtripsoptimizer.utils.JointControlerUtils;
import playground.thibautd.utils.BoxAndWhiskerXYNumberDataset;
import playground.thibautd.utils.WrapperChartUtil;

/**
 * output plots comparing the avg number of joint trips per agent
 * per clique size between two plan files.
 *
 * This is meant to compare the initial and the steady state.
 *
 * @author thibautd
 */
public class CompareAvgNJointTripsPerCliqueSize {
	private static final String MODULE = "seriesName";
	private static final String PARAM = "name";

	private static final int HEIGHT = 600;
	private static final int WIDTH = 800;

	/**
	 * usage: CompareAvgNJointTripsPerCliqueSize conf1 conf2 outpath
	 *
	 * the config files must have a module "seriesName" with a parameter "name"
	 */
	public static void main(final String[] args) {
		String configFile1 = args[0];
		String configFile2 = args[1];
		String outputPath = args[2];

		ScenarioWithCliques scenario1 = JointControlerUtils.createScenario(configFile1);
		ScenarioWithCliques scenario2 = JointControlerUtils.createScenario(configFile2);

		//BoxAndWhiskerXYNumberDataset dataset = new BoxAndWhiskerXYNumberDataset();

		//for (Map.Entry<Integer, List<Integer>> entry :
		//		getNJointTripsPerPerson(scenario1.getCliques()).entrySet()) {
		//	dataset.add(0, entry.getKey(), entry.getValue());
		//}
		//dataset.setSeriesKey(0, scenario1.getConfig().getModule(MODULE).getValue(PARAM));

		//for (Map.Entry<Integer, List<Integer>> entry :
		//		getNJointTripsPerPerson(scenario2.getCliques()).entrySet()) {
		//	dataset.add(1, entry.getKey(), entry.getValue());
		//}
		//dataset.setSeriesKey(1, scenario2.getConfig().getModule(MODULE).getValue(PARAM));

		////JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
		//JFreeChart chart = ChartFactory.createXYLineChart(
		//		"number of joint trips per individual",
		//		"clique size",
		//		"number of joint trips",
		//		dataset,
		//		PlotOrientation.VERTICAL,
		//		true,
		//		false,
		//		false);

		//(new WrapperChartUtil(chart)).saveAsPng(
		//		outputPath+"/nJointTripsPerCliqueSize.png",
		//		WIDTH,
		//		HEIGHT);
		XYLineChart chart = new XYLineChart(
				"average number of joint trips per individual",
				"clique size",
				"avg. number of joint trips");
		addSeries(scenario1, chart);
		addSeries(scenario2, chart);
		chart.getChart().getXYPlot().setRenderer(
				new XYLineAndShapeRenderer(
					true, // draw lines
					true)); // draw points
		chart.saveAsPng(
				outputPath+"/nJointTripsPerCliqueSize.png",
				WIDTH,
				HEIGHT);

		//chart = new XYLineChart(
		//		"joint trip acceptance rate",
		//		"clique size",
		//		"joint trips acceptance rate");
		//addSeries(scenario1, scenario2, chart);

		//chart.getChart().getXYPlot().setRenderer(
		//		new XYLineAndShapeRenderer(
		//			true, // draw lines
		//			true)); // draw points
		//chart.saveAsPng(
		//		outputPath+"/acceptanceRates.png",
		//		WIDTH,
		//		HEIGHT);

	}

	//private static void addSeries(
	//		final ScenarioWithCliques scenarioInit,
	//		final ScenarioWithCliques scenarioSteady,
	//		final XYLineChart chart) {
	//	SortedMap<Integer, Double> seriesInit = new TreeMap<Integer, Double>(getAverageNTrips(scenarioInit.getCliques()));
	//	SortedMap<Integer, Double> seriesSteady = new TreeMap<Integer, Double>(getAverageNTrips(scenarioSteady.getCliques()));
	//	double[] xSeries = new double[seriesInit.size()];
	//	double[] ySeries = new double[seriesInit.size()];
	//	int i=0;

	//	for (Map.Entry<Integer, Double> entry : seriesInit.entrySet()) {
	//		xSeries[i] = entry.getKey();
	//		ySeries[i] = seriesSteady.get(entry.getKey()) / entry.getValue();
	//		i++;
	//	}

	//	chart.addSeries(
	//		"acceptance rate",
	//		xSeries, ySeries);

	//}

	private static void addSeries(
			final ScenarioWithCliques scenario,
			final XYLineChart chart) {
		SortedMap<Integer, Double> series = new TreeMap<Integer, Double>(getAverageNTrips(scenario.getCliques()));
		double[] xSeries = new double[series.size()];
		double[] ySeries = new double[series.size()];
		int i=0;

		for (Map.Entry<Integer, Double> entry : series.entrySet()) {
			xSeries[i] = entry.getKey();
			ySeries[i] = entry.getValue();
			i++;
		}

		chart.addSeries(
			scenario.getConfig().getModule(MODULE).getValue(PARAM),
			xSeries, ySeries);

	}


	private static Map<Integer, List<Integer>> getNJointTripsPerPerson(
			final PopulationOfCliques cliques) {
		Map<Integer, List<Integer>> out = new HashMap<Integer, List<Integer>>();
		List<Integer> sizeCounts;
		int cliqueSize;

		for (Clique clique : cliques.getCliques().values()) {
			cliqueSize = clique.getMembers().size();
			sizeCounts = out.get(cliqueSize);

			if (sizeCounts == null) {
				sizeCounts = new ArrayList<Integer>();
				out.put(cliqueSize, sizeCounts);
			}

			for (Person person : clique.getMembers().values()) {
				sizeCounts.add(countJointTrips(person.getSelectedPlan()));
			}
		}

		return out;
	}

	private static Map<Integer, Double> getAverageNTrips(
			final PopulationOfCliques cliques) {
		Map<Integer, Double> out = new HashMap<Integer, Double>();
		double cumul;
		double count;

		for (Map.Entry<Integer, List<Integer>> entry :
				getNJointTripsPerPerson(cliques).entrySet()) {
			cumul = 0;
			count = 0;

			for (Integer n : entry.getValue()) {
				cumul += n;
				count++;
			}

			out.put(entry.getKey(), cumul/count);
		}

		return out;
	}

	private static int countJointTrips(final Plan plan) {
		int count = 0;

		for (PlanElement pe : plan.getPlanElements()) {
			if ((pe instanceof Activity) &&
					(JointActingTypes.DROP_OFF.equals(((Activity) pe).getType()))) {
				count++;
			}
		}

		return count;
	}
}

