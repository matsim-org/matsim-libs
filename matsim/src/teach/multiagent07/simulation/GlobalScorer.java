/* *********************************************************************** *
 * project: org.matsim.*
 * GlobalScorer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package teach.multiagent07.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;

import teach.multiagent07.interfaces.EventHandlerI;
import teach.multiagent07.population.Person;
import teach.multiagent07.population.Plan;
import teach.multiagent07.population.Population;
import teach.multiagent07.util.Event;

public class GlobalScorer  implements EventHandlerI{

	public GlobalScorer (Population pop) {
		this.population = pop;
	}

	Map<Id, Double> agentDeparture = new TreeMap<Id, Double>();
	private Population population;

	private final double beta_EARLY =  0.25/60;
	private final double beta_LATE =   1.5/60;
	private final double beta_TRAVEL = 0.4/60;
	private final double BEST_START = 6.5*3600; // 6:30 a.m.

	private final int TIMEBINSIZE = 1*60; // 1 min timebins

	private Map<Integer, Integer> count = new TreeMap<Integer, Integer>();
	private Map<Integer, Double> dpTimeSum = new HashMap<Integer, Double>();
	private Map<Integer, Double> utlSum = new HashMap<Integer, Double>();

	private double expSum = 0;


	private int timeToBin(double time) {
		return (int)(time/ TIMEBINSIZE);
	}

	private void addToTimeBin(double dpTime, double util_dp) {
		int timeBin = timeToBin(dpTime);

		if (count.get(timeBin) == null) {
			count.put(timeBin, 0);
			dpTimeSum.put(timeBin,0.);
			utlSum.put(timeBin, 0.);
		}

		count.put(timeBin, count.get(timeBin) + 1);
		dpTimeSum.put(timeBin, dpTimeSum.get(timeBin) + dpTime);
		utlSum.put(timeBin, utlSum.get(timeBin) + util_dp);
	}

	public void calculateAverage() {
		for (Integer timeBin : count.keySet()) {
			if(count.get(timeBin) > 0) {
				int anzahl = count.get(timeBin);
				// I put the average departure values into the old arrays
				dpTimeSum.put(timeBin, dpTimeSum.get(timeBin) / anzahl);
				utlSum.put(timeBin, utlSum.get(timeBin) / anzahl);

				// add this util to expsum;
				expSum += Math.exp(utlSum.get(timeBin));
			} else {
				// not so easy
			}
		}
	}

	public void printTimeBins() {
		for (Integer timeBin : count.keySet()) {
			if(count.get(timeBin) > 0) {
				int anzahl = count.get(timeBin);
				System.out.println("timebin: " + timeBin + " time" + dpTimeSum.get(timeBin)/3600 + " score " + utlSum.get(timeBin) + " count " + anzahl);
			}
		}
	}

	public double calcDepTime() {
		double rnd = Math.random();
		double sum = 0. ;
		Integer result = 0;

		for (Integer timeBin : count.keySet()) {
			sum += Math.exp(utlSum.get(timeBin)) / expSum ;
			if ( sum > rnd ) {
				result = timeBin;
				break ;
			}
		}

		return dpTimeSum.get(result);
	}

	public void handleEvent(Event event) {
		if(event.type == Event.ACT_DEPARTURE) {
			agentDeparture.put(event.agentId, new Double(event.time));

		}else if(event.type == Event.ACT_ARRIVAL) {
			Person person = population.getPerson(event.agentId);
			Plan plan = person.getSelectedPlan();
			double arrival = event.time;
			double departure = agentDeparture.get(event.agentId);

			double score = -beta_TRAVEL*(arrival - departure);

			if (event.legNumber == 1) {
				if (arrival < BEST_START) {
					// calc score with penalty for being early
					plan.setScore(score -Math.abs(beta_EARLY*(BEST_START - arrival)));
				} else {
					// calc score with penalty for being late
					plan.setScore(score -Math.abs(beta_LATE*(arrival - BEST_START)));
				}
				// sum all times up in the appropriate time bin
				addToTimeBin(departure, plan.getScore());
			}
		}
	}

}
