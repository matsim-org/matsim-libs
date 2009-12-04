/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityTypeDetector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author yu
 * 
 */
public class ActivityTypeDetector extends AbstractPersonAlgorithm implements
		PlanAlgorithm {
	private Map<String, Double> shortestDurs = new HashMap<String, Double>();
	private Map<String, Double> longestDurs = new HashMap<String, Double>();
	private Map<String, Double> earlistEndTimes = new HashMap<String, Double>();
	private Map<String, Double> latestEndTimes = new HashMap<String, Double>();
	private Map<String, Double> earlistStartTimes = new HashMap<String, Double>();
	private Map<String, Double> latestStartTimes = new HashMap<String, Double>();
	private Map<String, Tuple<Integer, Double>> avgDurs = new HashMap<String, Tuple<Integer, Double>>();

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans())
			run(plan);
	}

	public void run(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl) pe;
				String actType = act.getType();
				if ((!actType.equals("H")) && (!actType.equals("W"))) {
					// duration
					double dur = act.getDuration();

					Double shortestDur = shortestDurs.get(actType);
					if (shortestDur != null && shortestDur <= dur) {
					} else
						shortestDurs.put(actType, dur);

					Double longestDur = longestDurs.get(actType);
					if (longestDur != null && longestDur >= dur) {
					} else
						longestDurs.put(actType, dur);

					Tuple<Integer, Double> tuple = avgDurs.get(actType);
					if (tuple == null)
						tuple = new Tuple<Integer, Double>(0, 0.0);
					avgDurs.put(actType, new Tuple<Integer, Double>(tuple
							.getFirst() + 1, tuple.getSecond() + dur));
					// endTime
					double endTime = act.getEndTime();

					Double earlistEndTime = earlistEndTimes.get(actType);
					if (earlistEndTime != null && earlistEndTime <= endTime) {
					} else
						earlistEndTimes.put(actType, endTime);

					Double latestEndTime = latestEndTimes.get(actType);
					if (latestEndTime != null && latestEndTime >= endTime) {
					} else
						latestEndTimes.put(actType, endTime);
					// startTime
					double startTime = endTime - dur;

					Double earlistStartTime = earlistStartTimes.get(actType);
					if (earlistStartTime != null
							&& earlistStartTime <= startTime) {
					} else
						earlistStartTimes.put(actType, startTime);

					Double latestStartTime = latestStartTimes.get(actType);
					if (latestStartTime != null && latestStartTime >= startTime) {
					} else
						latestStartTimes.put(actType, startTime);
				}
			}
		}
	}

	public void output() {
		System.out.println(shortestDurs.keySet());
		System.out.println("-----earliestEndTimes-----");
		for (Entry<String, Double> entry : earlistEndTimes.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----earliestStartTimes-----");
		for (Entry<String, Double> entry : earlistStartTimes.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----latestEndTimes-----");
		for (Entry<String, Double> entry : latestEndTimes.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----latestStartTimes-----");
		for (Entry<String, Double> entry : latestStartTimes.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----longestDurations-----");
		for (Entry<String, Double> entry : longestDurs.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----shortestDurations-----");
		for (Entry<String, Double> entry : shortestDurs.entrySet()) {
			System.out.println(entry);
		}
		System.out.println("-----avg.Durations-----");
		for (Entry<String, Tuple<Integer, Double>> entry : avgDurs.entrySet()) {
			Tuple<Integer, Double> tuple = entry.getValue();
			System.out.println(entry.getKey() + " : "
					+ tuple.getSecond().doubleValue()
					/ tuple.getFirst().doubleValue());
		}
		System.out.println("done!");
	}

	public static void main(String[] args) {
		ScenarioImpl s = new ScenarioImpl();

		new MatsimNetworkReader(s.getNetwork())
				.readFile("D:/fromNB04/wm/Toronto/toronto/networks/changedNetworkWithManeuvers/network.xml");

		new MatsimPopulationReader(s)
				.readFile("D:/fromNB04/wm/Toronto/toronto/plans/xy/plans.xml.gz");

		ActivityTypeDetector atd = new ActivityTypeDetector();
		atd.run(s.getPopulation());
		atd.output();
	}
}
