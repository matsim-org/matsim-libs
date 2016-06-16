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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashSet;
import java.util.Set;

/**
 * WHAT IS IT FOR?
 *
 * @author boescpa
 */
public class ScoringAnalyzer extends PopulationAnalyzer {

	private int highScoreCounter;
	private int lowScoreCounter;
	private Double highScoreSum;
	private Double lowScoreSum;
	private Set<String> typesOfLowScorers;
	private int sameActScoreCounter;
	private Double sameActScoreSum;
	private int firstHomeScoreCounter;
	private Double firstHomeScoreSum;
	private int otherActScoreCounter;
	private Double otherActScoreSum;

	public ScoringAnalyzer(String pop2bAnalyzed) {
		super(pop2bAnalyzed);
		getCharts = false;
	}

	public static void main(final String[] args) {
		final String pop2bAnalyzed = args[0];
		final String resultsDest = args[1];
		new ScoringAnalyzer(pop2bAnalyzed).analyzePopulation(resultsDest, null);
	}

	@Override
	protected void reset() {
		highScoreCounter = 0;
		lowScoreCounter = 0;
		highScoreSum = 0.;
		lowScoreSum = 0.;
		typesOfLowScorers = new HashSet<>();
		sameActScoreCounter = 0;
		sameActScoreSum = 0.;
		firstHomeScoreCounter = 0;
		firstHomeScoreSum = 0.;
		otherActScoreCounter = 0;
		otherActScoreSum = 0.;
	}

	@Override
	protected void analyzeAgent(Person person) {
		modes.add("total");
		person.getPlans().stream().forEach(this::handlePlan);
	}

	private void handlePlan(Plan plan) {
		if (plan.getScore() > -10000) {
			this.highScoreCounter++;
			this.highScoreSum += plan.getScore();
		} else {
			this.lowScoreCounter++;
			this.lowScoreSum += plan.getScore();
			Object subpopTag = plan.getPerson().getCustomAttributes().get("subpopulation");
			if (subpopTag != null) {
				this.typesOfLowScorers.add((String) subpopTag);
			} else {
				this.typesOfLowScorers.add("main");
			}
		}
		Activity firstAct = (Activity)plan.getPlanElements().get(0);
		Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size()-1);
		if (firstAct.getType().equals(lastAct.getType())) {
			this.sameActScoreCounter++;
			this.sameActScoreSum += plan.getScore();
		} else if (firstAct.getType().equals("home")) {
			this.firstHomeScoreCounter++;
			this.firstHomeScoreSum += plan.getScore();
		} else {
			this.otherActScoreCounter++;
			this.otherActScoreSum += plan.getScore();
		}
	}

	@Override
	protected String getResultString(String mode) {
		String outputString = "Number of high-scorer: " + this.highScoreCounter + "\n"
				+ "Average of high-scorer: " + (this.highScoreSum/this.highScoreCounter) + "\n"
				+ "Number of low-scorer: " + this.lowScoreCounter + "\n"
				+ "Average of low-scorer: " + (this.lowScoreSum/this.lowScoreCounter) + "\n"
				+ "Types of low-scorer: ";
		for (String subpopType : this.typesOfLowScorers) {
			outputString = outputString + subpopType + ", ";
		}
		outputString = outputString + "\n" + "\n" + "Average score of first and last same act: " + (this.sameActScoreSum/this.sameActScoreCounter);
		outputString = outputString + "\n" + "Average score of first home act, last act different: " + (this.firstHomeScoreSum/this.firstHomeScoreCounter);
		outputString = outputString + "\n" + "Average score of first not home act, last act different: " + (this.otherActScoreSum/this.otherActScoreCounter);
		return outputString;
	}

	@Override
	protected Tuple<String, double[]> getSeries(String mode) {
		return null;
	}
}
