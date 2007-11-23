/* *********************************************************************** *
 * project: org.matsim.*
 * Scorer.java
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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.utils.identifiers.IdI;

import teach.multiagent07.interfaces.EventHandlerI;
import teach.multiagent07.population.Person;
import teach.multiagent07.population.Plan;
import teach.multiagent07.population.Population;
import teach.multiagent07.util.Event;

public class Scorer  implements EventHandlerI{
	Map<IdI, Double> agentDeparture = new TreeMap<IdI, Double>();
	private Population population;

	private final double beta_EARLY =  0.25/60;
	private final double beta_LATE =   1.5/60;
	private final double beta_TRAVEL = 0.4/60;
	private final double BEST_START = 6.5*3600; // 6:30 a.m.

	public Scorer (Population pop) {
		this.population = pop;
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
					plan.setScore(score -Math.abs(beta_EARLY*(arrival - BEST_START)));
				}
			}
		}
	}

}
