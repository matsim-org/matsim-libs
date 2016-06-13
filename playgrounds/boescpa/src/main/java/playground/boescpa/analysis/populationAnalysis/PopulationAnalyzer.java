/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.analysis.populationAnalysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.lib.tools.PopulationUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides a template for parallel population analysis.
 *
 * @author boescpa
 */
public abstract class PopulationAnalyzer {
	protected final static Logger log = Logger.getLogger(PopulationAnalyzer.class);

	protected boolean getCharts = false;
	protected final Set<String> modes = new HashSet<>();
	private Population population;
	private String activityType = null;

	public PopulationAnalyzer(Population population) {
		this.population = population;
	}

	public PopulationAnalyzer(final String pop2bAnalyzed) {
		this.population = PopulationUtils.readPopulation(pop2bAnalyzed);
	}

	protected String getActivityType() {
		return activityType;
	}

	/**
	 * Analyze the full population independently of activity type.
	 */
	public void analyzePopulation(final String resultsDest) {
		analyzePopulation(resultsDest, null);
	}

	/**
	 * Analyze the full population for the specified activity type.
	 */
	public void analyzePopulation(final String resultsDest, final String activityType) {
		this.activityType = activityType;
		resetMain();
		reset();
		analyzeAgents();
		writeResults(resultsDest);
	}

	private void resetMain() {
		modes.clear();
	}

	protected abstract void reset();

	private void analyzeAgents() {
		this.population.getPersons().values().parallelStream().forEach(this::analyzeAgent);
		//this.population.getPersons().values().stream().forEach(this::analyzeAgent);
	}

	protected abstract void analyzeAgent(Person person);

	private void writeResults(String resultsDest) {
		// title
		String title;
		if (activityType == null) {
			title = "ANALYSIS FOR ALL ACTIVITIES";
		} else {
			title = "ANALYSIS FOR " + activityType.toUpperCase() + "-TRIPS";
		}
		// tables
		BufferedWriter writer = IOUtils.getBufferedWriter(resultsDest);
		try {
			writer.write(title);
			writer.newLine();
			for (String mode : modes) {
				log.info(getResultString(mode));
				writer.newLine();
				writer.write(getResultString(mode));
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// charts
		if (getCharts) {
			BarChart chart = new BarChart(title, "", "# of occurances");
			for (String mode : modes) {
				Tuple<String, double[]> series = getSeries(mode);
				chart.addSeries(series.getFirst(), series.getSecond());
			}
			chart.saveAsPng(resultsDest.substring(0, resultsDest.lastIndexOf(".")) + ".png", 800, 600);
		}
	}

	protected abstract String getResultString(String mode);

	/**
	 * @return Either a "series" consisting of a String and a double[], or null, which is understood as "no charts".
	 */
	protected abstract Tuple<String, double[]> getSeries(String mode);
}
