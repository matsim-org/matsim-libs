/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * 
 * @author ikaddoura
 *
 */
public class CongestionAnalysisHandler implements CongestionEventHandler {
	private final static Logger log = Logger.getLogger(CongestionAnalysisHandler.class);

	private BasicPersonTripAnalysisHandler basicHandler;
	private boolean caughtCongestionEvent = false;
	
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2causedDelay = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2affectedDelay = new HashMap<>();

	public CongestionAnalysisHandler(BasicPersonTripAnalysisHandler basicHandler) {
		this.basicHandler = basicHandler;
		if (this.basicHandler == null) {
			log.warn("The provided handler is null. This may result in a runtime exception.");
		}
	}

	@Override
	public void reset(int iteration) {
		personId2tripNumber2causedDelay.clear();
		personId2tripNumber2affectedDelay.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {
								
		if (caughtCongestionEvent == false) {
			log.info("At least one congestion event was handled.");
			caughtCongestionEvent = true;
		}
		
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
			
		// caused delay
		
		if (personId2tripNumber2causedDelay.containsKey(event.getCausingAgentId()) && personId2tripNumber2causedDelay.get(event.getCausingAgentId()).containsKey(tripNumber) ) {
		
			double causedDelayBefore = personId2tripNumber2causedDelay.get(event.getCausingAgentId()).get(tripNumber);
			double causedDelayUpdated = causedDelayBefore + event.getDelay();
			
			Map<Integer,Double> tripNumber2causedDelay = personId2tripNumber2causedDelay.get(event.getCausingAgentId());
			tripNumber2causedDelay.put(tripNumber, causedDelayUpdated);
			personId2tripNumber2causedDelay.put(event.getCausingAgentId(), tripNumber2causedDelay);
		
		} else {
			
			Map<Integer,Double> tripNumber2causedDelay = new HashMap<>();
			tripNumber2causedDelay.put(tripNumber, event.getDelay());
			personId2tripNumber2causedDelay.put(event.getCausingAgentId(), tripNumber2causedDelay);

		}
		
		// affected delay
		
		if (personId2tripNumber2affectedDelay.containsKey(event.getAffectedAgentId()) && personId2tripNumber2affectedDelay.get(event.getAffectedAgentId()).containsKey(tripNumber)) {
		
			double affectedDelayBefore = personId2tripNumber2affectedDelay.get(event.getAffectedAgentId()).get(tripNumber);
			double affectedDelayUpdated = affectedDelayBefore + event.getDelay();
			
			Map<Integer,Double> tripNumber2affectedDelay = personId2tripNumber2affectedDelay.get(event.getAffectedAgentId());
			tripNumber2affectedDelay.put(tripNumber, affectedDelayUpdated);
			personId2tripNumber2affectedDelay.put(event.getAffectedAgentId(), tripNumber2affectedDelay);
		
		} else {
			
			Map<Integer,Double> tripNumber2affectedDelay = new HashMap<>();
			tripNumber2affectedDelay.put(tripNumber, event.getDelay());
			personId2tripNumber2affectedDelay.put(event.getAffectedAgentId(), tripNumber2affectedDelay);
			
		}	
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2causedDelay() {
		return personId2tripNumber2causedDelay;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2affectedDelay() {
		return personId2tripNumber2affectedDelay;
	}

	public boolean isCaughtCongestionEvent() {
		return caughtCongestionEvent;
	}
	
}
