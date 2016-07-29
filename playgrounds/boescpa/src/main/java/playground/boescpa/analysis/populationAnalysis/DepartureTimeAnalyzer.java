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

import org.matsim.api.core.v01.population.*;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Analyses departure times for a given plans-file.
 *
 * @author boescpa
 */
public class DepartureTimeAnalyzer extends PopulationAnalyzer{

	// Flag determining if only the first departure of the day for each activity should be recorded or if all departures.
	private final static boolean ONLY_FIRST_DEPARTURE_OF_DAY = false;

	private final Map<String, long[]> depTimes = new HashMap<>();
	private final int statisticInterval = 60; // mins

	public DepartureTimeAnalyzer(Population population) {
		super(population);
		getCharts = true;
	}

	public DepartureTimeAnalyzer(String pop2bAnalyzed) {
		super(pop2bAnalyzed);
		getCharts = true;
	}

	public static void main(final String[] args) {
		final String pop2bAnalyzed = args[0];
		final String resultsDest = args[1];
		final String actTypeToAnalyze = args.length > 2 ? args[2] : null;
		new DepartureTimeAnalyzer(pop2bAnalyzed).analyzePopulation(resultsDest, actTypeToAnalyze);
	}

	@Override
	final protected void reset() {
		depTimes.clear();
	}

	@Override
	final protected void analyzeAgent(Person person) {
		double formerActDepTime = 0;
		String mode = null;
		Set<String> alreadyCheckedOnThisDay = new HashSet<>();
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (!activity.getType().equals("pt interaction")) {
					if (mode != null
							&& (getActivityType() == null || activity.getType().equals(getActivityType()))
							&& firstDepartureCheck(alreadyCheckedOnThisDay, activity)) {
						classifyDepTime(formerActDepTime, "total");
						classifyDepTime(formerActDepTime, mode);
					}
					formerActDepTime = activity.getEndTime();
				}
			} else if (planElement instanceof Leg){
				mode = ((Leg)planElement).getMode();
			} else {
				log.error("Unhandled implementation of PlanElement: " + planElement.toString());
			}
		}
	}

	private boolean firstDepartureCheck(Set<String> alreadyCheckedOnThisDay, Activity activity) {
		return !ONLY_FIRST_DEPARTURE_OF_DAY || alreadyCheckedOnThisDay.add(activity.getType());
	}

	private void classifyDepTime(double formerActDepTime, String mode) {
		double corrDepTime = formerActDepTime > (30*60*60) ? (30*60*60) : formerActDepTime;
		int depTime = (int)Math.floor(corrDepTime/(statisticInterval*60));
		synchronized (depTimes) {
			if (!depTimes.keySet().contains(mode)) {
				depTimes.put(mode, new long[(30*60/statisticInterval) + 1]);
				modes.add(mode);
			}
			depTimes.get(mode)[depTime]++;
		}
	}

	@Override
	final protected String getResultString(final String mode) {
		String modeString = "************************" + "\n" + "departures " + mode + ": " + "\n" + "\n";
		// times
		modeString = modeString + "time  " + "\t";
		for (int i = 0; i < depTimes.get(mode).length; i++) {
			int hour = i*statisticInterval/60;
			int min = (i*statisticInterval)-(hour*60);
			modeString = modeString + hour + ":" + min + "\t";
		}
		modeString = modeString + "\n";
		// counts
		modeString = modeString + "counts" + "\t";
		for (int i = 0; i < depTimes.get(mode).length; i++) {
			modeString = modeString + depTimes.get(mode)[i] + "\t";
		}
		modeString = modeString + "\n";
		return modeString;
	}

	@Override
	final protected Tuple<String, double[]> getSeries(String mode) {
		double[] series = new double[depTimes.get(mode).length];
		for (int i = 0; i < depTimes.get(mode).length; i++) {
			series[i] = depTimes.get(mode)[i];
		}
		return new Tuple<>("departures " + mode, series);
	}
}
