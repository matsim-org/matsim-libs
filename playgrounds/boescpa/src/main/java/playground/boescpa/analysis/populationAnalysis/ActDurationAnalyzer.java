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
import java.util.Map;

/**
 * Analyses activity durations for a given plans-file.
 *
 * @author boescpa
 */
public class ActDurationAnalyzer extends PopulationAnalyzer{

	// Flag to record the durations of the beginning of the first activity of a type until
	// the end of the last activity of the same type (<-> all of the same type as one).
	private final static boolean ALL_SAME_TYPE_ACTS_AS_ONE = false;

	private final Map<String, long[]> durations = new HashMap<>();
	private final int statisticInterval = 30; // mins

	public ActDurationAnalyzer(Population population) {
		super(population);
		getCharts = true;
	}

	public ActDurationAnalyzer(String pop2bAnalyzed) {
		super(pop2bAnalyzed);
		getCharts = true;
	}

	public static void main(final String[] args) {
		final String pop2bAnalyzed = args[0];
		final String resultsDest = args[1];
		final String actTypeToAnalyze = args.length > 2 ? args[2] : null;
		new ActDurationAnalyzer(pop2bAnalyzed).analyzePopulation(resultsDest, actTypeToAnalyze);
	}

	@Override
	final protected void reset() {
		durations.clear();
	}

	@Override
	final protected void analyzeAgent(Person person) {
		Map<String, Tuple<Double, Double>> sameTypeStartEnd = new HashMap<>();
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				if (getActivityType() == null || activity.getType().equals(getActivityType())) {
					if (activity.getStartTime() != Time.UNDEFINED_TIME && activity.getEndTime() != Time.UNDEFINED_TIME) {
						if (!ALL_SAME_TYPE_ACTS_AS_ONE) {
							evaluateActivity(activity.getType(), activity.getStartTime(), activity.getEndTime());
						} else {
							if (!sameTypeStartEnd.keySet().contains(activity.getType())) {
								sameTypeStartEnd.put(activity.getType(), new Tuple<>(activity.getStartTime(), activity.getEndTime()));
							} else {
								double firstStartTime = sameTypeStartEnd.get(activity.getType()).getFirst();
								sameTypeStartEnd.put(activity.getType(), new Tuple<>(firstStartTime, activity.getEndTime()));
							}
						}
					}
				}
			} else if (planElement instanceof Leg){
			} else {
				log.error("Unhandled implementation of PlanElement: " + planElement.toString());
			}
		}
		if (ALL_SAME_TYPE_ACTS_AS_ONE) {
			for (String actType : sameTypeStartEnd.keySet()) {
				evaluateActivity(actType, sameTypeStartEnd.get(actType).getFirst(), sameTypeStartEnd.get(actType).getSecond());
			}
		}
	}

	private void evaluateActivity(String actType, double startTime, double endTime) {
		double duration = endTime - startTime;
		duration = duration < 1 ? 1 : duration;
		if (getActivityType() == null) classifyDuration(duration, "total");
		classifyDuration(duration, actType);
	}

	private void classifyDuration(double duration, String activity) {
		int durInterval = (int) duration / (60*statisticInterval);
		synchronized (durations) {
			if (!durations.keySet().contains(activity)) {
				durations.put(activity, new long[(30*60/statisticInterval) + 1]);
				modes.add(activity);
			}
			durations.get(activity)[durInterval]++;
		}
	}

	@Override
	final protected String getResultString(final String activity) {
		String resultString = "************************" + "\n" + "departures " + activity + ": " + "\n" + "\n";
		// intervals
		resultString = resultString + "duration" + "\t";
		for (int i = 0; i < durations.get(activity).length; i++) {
			double duration = i*statisticInterval/60.0;
			resultString = resultString + duration + "\t";
		}
		resultString = resultString + "\n";
		// counts
		resultString = resultString + "counts  " + "\t";
		for (int i = 0; i < durations.get(activity).length; i++) {
			resultString = resultString + durations.get(activity)[i] + "\t";
		}
		resultString = resultString + "\n";
		return resultString;
	}

	@Override
	final protected Tuple<String, double[]> getSeries(String activity) {
		double[] series = new double[durations.get(activity).length];
		for (int i = 0; i < durations.get(activity).length; i++) {
			series[i] = durations.get(activity)[i];
		}
		return new Tuple<>("durations " + activity, series);
	}
}
