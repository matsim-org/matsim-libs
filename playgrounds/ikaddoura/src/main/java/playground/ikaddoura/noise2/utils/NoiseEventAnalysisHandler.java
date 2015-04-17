/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise2.utils;

import java.util.SortedMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.noise2.events.NoiseEventAffected;
import playground.ikaddoura.noise2.events.NoiseEventCaused;
import playground.ikaddoura.noise2.handler.NoiseEventAffectedHandler;
import playground.ikaddoura.noise2.handler.NoiseEventCausedHandler;

/**
 * Analyzes an event file with noise events.
 * 
 * @author ikaddoura
 *
 */
public class NoiseEventAnalysisHandler implements NoiseEventCausedHandler, NoiseEventAffectedHandler {

	private SortedMap<Id<Person>, Double> id2causedNoiseCost = new TreeMap<Id<Person>, Double>();
	private SortedMap<Id<Person>, Double> id2affectedNoiseCost = new TreeMap<Id<Person>, Double>();

	@Override
	public void reset(int iteration) {
		id2causedNoiseCost.clear();
		id2affectedNoiseCost.clear();
	}

	@Override
	public void handleEvent(NoiseEventCaused event) {

		Id<Person> id = event.getCausingAgentId();
		Double amountByEvent = event.getAmount();
		Double amountSoFar = id2causedNoiseCost.get(id);
		
		if (amountSoFar == null) {
			id2causedNoiseCost.put(id, amountByEvent);
		}
		else {
			amountSoFar += amountByEvent;
			id2causedNoiseCost.put(id, amountSoFar);
		}	
		
	}

	@Override
	public void handleEvent(NoiseEventAffected event) {
		Id<Person> id = event.getAffectedAgentId();
		Double amountByEvent = event.getAmount();
		Double amountSoFar = id2affectedNoiseCost.get(id);
		
		if (amountSoFar == null) {
			id2affectedNoiseCost.put(id, amountByEvent);
		}
		else {
			amountSoFar += amountByEvent;
			id2affectedNoiseCost.put(id, amountSoFar);
		}
	}

	public SortedMap<Id<Person>, Double> getPersonId2causedNoiseCost() {
		return id2causedNoiseCost;
	}

	public SortedMap<Id<Person>, Double> getPersonId2affectedNoiseCost() {
		return id2affectedNoiseCost;
	}

}
