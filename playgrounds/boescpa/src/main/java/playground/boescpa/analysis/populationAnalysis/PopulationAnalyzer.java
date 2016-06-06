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
import playground.boescpa.lib.tools.PopulationUtils;

/**
 * Provides a template for parallel population analysis.
 *
 * @author boescpa
 */
public abstract class PopulationAnalyzer {
	protected final static Logger log = Logger.getLogger(PopulationAnalyzer.class);

	private Population population;

	public static void analyzePopulation(final PopulationAnalyzer analyzer, final String pop2bAnalyzed, final String resultsDest) {
		analyzer.readPopulation(pop2bAnalyzed);
		analyzer.analyzeAgents();
		analyzer.writeResults(resultsDest);
	}

	private void readPopulation(String pop2bAnalyzed) {
		this.population = PopulationUtils.readPopulation(pop2bAnalyzed);
	}

	private void analyzeAgents() {
		this.population.getPersons().values().parallelStream().forEach(this::analyzeAgent);
	}

	protected abstract void analyzeAgent(Person person);

	protected abstract void writeResults(String resultsDest);
}
