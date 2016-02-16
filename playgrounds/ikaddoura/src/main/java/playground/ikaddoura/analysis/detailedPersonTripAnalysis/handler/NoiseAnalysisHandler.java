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
package playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.events.NoiseEventAffected;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.contrib.noise.handler.NoiseEventAffectedHandler;
import org.matsim.contrib.noise.handler.NoiseEventCausedHandler;

/**
 * Analyzes an event file with noise events.
 * 
 * @author ikaddoura
 *
 */
public class NoiseAnalysisHandler implements NoiseEventCausedHandler, NoiseEventAffectedHandler {
	private final static Logger log = Logger.getLogger(NoiseAnalysisHandler.class);

	private BasicPersonTripAnalysisHandler basicHandler;
	
	private boolean caughtNoiseEvent = false;
	
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2causedNoiseCost = new HashMap<>();
	private SortedMap<Id<Person>, Double> id2causedNoiseCost = new TreeMap<Id<Person>, Double>();
	private SortedMap<Id<Person>, Double> id2affectedNoiseCost = new TreeMap<Id<Person>, Double>();

	private double causedNoiseCost = 0.;
	private double affectedNoiseCost = 0.;
	
	public NoiseAnalysisHandler(BasicPersonTripAnalysisHandler basicHandler) {
		this.basicHandler = basicHandler;
	}

	@Override
	public void reset(int iteration) {
		personId2tripNumber2causedNoiseCost.clear();
		id2causedNoiseCost.clear();
		id2affectedNoiseCost.clear();
		causedNoiseCost = 0.;
		affectedNoiseCost = 0.;
	}

	@Override
	public void handleEvent(NoiseEventCaused event) {
		
		if (caughtNoiseEvent == false) {
			log.info("At least one noise event was handled.");
			caughtNoiseEvent = true;
		}

		causedNoiseCost = causedNoiseCost + event.getAmount();
		
		Double amountSoFar = id2causedNoiseCost.get(event.getCausingAgentId());
		if (amountSoFar == null) {
			id2causedNoiseCost.put(event.getCausingAgentId(), event.getAmount());
		}
		else {
			id2causedNoiseCost.put(event.getCausingAgentId(), amountSoFar + event.getAmount());
		}	
		
		// trip-specific noise cost
		
		// get the right trip no. based on the emergence time
		
		int tripNumber = 0;
		double maxDepTime = 0.;
		
		for (int tripNr : basicHandler.getPersonId2tripNumber2departureTime().get(event.getCausingAgentId()).keySet()) {
			if (event.getEmergenceTime() >= basicHandler.getPersonId2tripNumber2departureTime().get(event.getCausingAgentId()).get(tripNr)) {
				if (basicHandler.getPersonId2tripNumber2departureTime().get(event.getCausingAgentId()).get(tripNr) >= maxDepTime) {
					tripNumber = tripNr;
				}
			}
		}
		
		if (personId2tripNumber2causedNoiseCost.containsKey(event.getCausingAgentId())) {
			
			if (personId2tripNumber2causedNoiseCost.get(event.getCausingAgentId()).containsKey(tripNumber)) {
				
				double causedAmountBefore = personId2tripNumber2causedNoiseCost.get(event.getCausingAgentId()).get(tripNumber);
				double causedAmountUpdated = causedAmountBefore + event.getAmount();
				
				Map<Integer,Double> tripNumber2causedAmount = personId2tripNumber2causedNoiseCost.get(event.getCausingAgentId());
				tripNumber2causedAmount.put(tripNumber, causedAmountUpdated);
				personId2tripNumber2causedNoiseCost.put(event.getCausingAgentId(), tripNumber2causedAmount);
				
			} else {
				Map<Integer,Double> tripNumber2causedDelay = personId2tripNumber2causedNoiseCost.get(event.getCausingAgentId());
				tripNumber2causedDelay.put(tripNumber, event.getAmount());
				personId2tripNumber2causedNoiseCost.put(event.getCausingAgentId(), tripNumber2causedDelay);
			}
			
		} else {
			
			Map<Integer,Double> tripNumber2causedDelay = new HashMap<>();
			tripNumber2causedDelay.put(tripNumber, event.getAmount());
			personId2tripNumber2causedNoiseCost.put(event.getCausingAgentId(), tripNumber2causedDelay);
		}		
	}

	@Override
	public void handleEvent(NoiseEventAffected event) {
		
		if (caughtNoiseEvent == false) {
			log.info("At least one noise event was handled.");
			caughtNoiseEvent = true;
		}
		
		affectedNoiseCost = affectedNoiseCost + event.getAmount();
		
		Double amountSoFar = id2affectedNoiseCost.get(event.getAffectedAgentId());
		if (amountSoFar == null) {
			id2affectedNoiseCost.put(event.getAffectedAgentId(), event.getAmount());
		}
		else {
			id2affectedNoiseCost.put(event.getAffectedAgentId(), amountSoFar + event.getAmount());
		}
	}

	public SortedMap<Id<Person>, Double> getPersonId2causedNoiseCost() {
		return id2causedNoiseCost;
	}

	public SortedMap<Id<Person>, Double> getPersonId2affectedNoiseCost() {
		return id2affectedNoiseCost;
	}

	public Map<Id<Person>,Map<Integer,Double>> getPersonId2tripNumber2causedNoiseCost() {
		// the trip number may be wrong because of the interval based computation of noise
		return personId2tripNumber2causedNoiseCost;
	}

	public boolean isCaughtNoiseEvent() {
		return caughtNoiseEvent;
	}

	public double getCausedNoiseCost() {
		return causedNoiseCost;
	}

	public double getAffectedNoiseCost() {
		return affectedNoiseCost;
	}
	
}
