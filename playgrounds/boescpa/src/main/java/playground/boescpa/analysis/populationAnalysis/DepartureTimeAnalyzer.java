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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analyses departure times for a given plans-file.
 *
 * @author boescpa
 */
public class DepartureTimeAnalyzer extends PopulationAnalyzer{

	private final Map<String, long[]> depTimes = new HashMap<>();

	public static void main(final String[] args) {
		final String pop2bAnalyzed = args[0];
		final String resultsDest = args[1];
		PopulationAnalyzer.analyzePopulation(new DepartureTimeAnalyzer(), pop2bAnalyzed, resultsDest);
	}

	@Override
	final protected void analyzeAgent(Person person) {
		double formerActDepTime = 0;
		for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
			if (planElement instanceof Activity) {
				Activity activity = (Activity) planElement;
				formerActDepTime = activity.getEndTime();
			} else if (planElement instanceof Leg){
				classifyDepTime(formerActDepTime, "total");
				classifyDepTime(formerActDepTime, ((Leg)planElement).getMode());
			} else {
				log.error("Unhandled implementation of PlanElement: " + planElement.toString());
			}
		}
	}

	private void classifyDepTime(double formerActDepTime, String mode) {
		double corrDepTime = formerActDepTime > (30*60*60) ? (30*60*60) : formerActDepTime;
		int depTime = (int)Math.floor(corrDepTime/(15*60));
		synchronized (depTimes) {
			if (!depTimes.keySet().contains(mode)) {
				depTimes.put(mode, new long[(30*60/15) + 1]);
			}
			depTimes.get(mode)[depTime]++;
		}
	}

	@Override
	final protected void writeResults(String resultsDest) {
		BufferedWriter writer = IOUtils.getBufferedWriter(resultsDest);
		try {
			for (String mode : depTimes.keySet()) {
				writer.write(getResultString(mode));
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getResultString(final String mode) {
		String modeString = "************************" + "\n" + "departures " + mode + ": " + "\n" + "\n";
		// times
		modeString = modeString + "time  " + "\t";
		for (int i = 0; i < depTimes.get(mode).length; i++) {
			int hour = i*15/60;
			int min = (i*15)-(hour*60);
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
}
