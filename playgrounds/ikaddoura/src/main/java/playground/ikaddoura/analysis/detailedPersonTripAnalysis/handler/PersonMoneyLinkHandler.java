/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEventHandler;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class PersonMoneyLinkHandler implements PersonLinkMoneyEventHandler {
	
	private final static Logger log = Logger.getLogger(PersonMoneyLinkHandler.class);
	
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2congestionPayment = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2noisePayment = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2airPollutionPayment = new HashMap<>();
	
	@Inject
	private BasicPersonTripAnalysisHandler basicHandler;
	
	private int warnCnt0 = 0;
	
	public void setBasicHandler(BasicPersonTripAnalysisHandler basicHandler) {
		this.basicHandler = basicHandler;
	}

	@Override
	public void reset(int iteration) {
		personId2tripNumber2congestionPayment.clear();
		personId2tripNumber2noisePayment.clear();
		personId2tripNumber2airPollutionPayment.clear();		
	}
	
	@Override
	public void handleEvent(PersonLinkMoneyEvent event) {
		
		
		if (this.basicHandler.getTaxiDrivers().contains(event.getPersonId()) || this.basicHandler.getPtDrivers().contains(event.getPersonId())) {
			if (warnCnt0 <= 5) {
				log.warn("A person link money event is thrown for a public tranist driver or taxi driver: " + event.toString());
				if (warnCnt0 == 5) {
					log.warn("Further warnings of this type are not printed out.");
				}
				warnCnt0++;
			}
			
		} else {
			int tripNumber = this.basicHandler.getPersonId2currentTripNumber().get(event.getPersonId());
			
			if (event.getDescription().equalsIgnoreCase("congestion")) {
				
				double paymentBefore = 0.;
				if (personId2tripNumber2congestionPayment.containsKey(event.getPersonId()) && personId2tripNumber2congestionPayment.get(event.getPersonId()).containsKey(tripNumber)) {
					paymentBefore = personId2tripNumber2congestionPayment.get(event.getPersonId()).get(tripNumber);
				}
				double updatedPayment = paymentBefore + (-1. * event.getAmount());
				
				Map<Integer,Double> tripNumber2payment = null;
				if (personId2tripNumber2congestionPayment.containsKey(event.getPersonId())) {
					tripNumber2payment = personId2tripNumber2congestionPayment.get(event.getPersonId());
				} else {
					tripNumber2payment = new HashMap<>();
				}
				
				tripNumber2payment.put(tripNumber, updatedPayment);
				personId2tripNumber2congestionPayment.put(event.getPersonId(), tripNumber2payment);
				
			} else if (event.getDescription().equalsIgnoreCase("noise")) {
				
				double paymentBefore = 0.;
				if (personId2tripNumber2noisePayment.containsKey(event.getPersonId()) && personId2tripNumber2noisePayment.get(event.getPersonId()).containsKey(tripNumber)) {
					paymentBefore = personId2tripNumber2noisePayment.get(event.getPersonId()).get(tripNumber);
				}
				double updatedPayment = paymentBefore + (-1. * event.getAmount());
				
				Map<Integer,Double> tripNumber2payment = null;
				if (personId2tripNumber2noisePayment.containsKey(event.getPersonId())) {
					tripNumber2payment = personId2tripNumber2noisePayment.get(event.getPersonId());
				} else {
					tripNumber2payment = new HashMap<>();
				}
				
				tripNumber2payment.put(tripNumber, updatedPayment);
				personId2tripNumber2noisePayment.put(event.getPersonId(), tripNumber2payment);
				
			} else if (event.getDescription().equalsIgnoreCase("airPollution")) {
				
				double paymentBefore = 0.;
				if (personId2tripNumber2airPollutionPayment.containsKey(event.getPersonId()) && personId2tripNumber2airPollutionPayment.get(event.getPersonId()).containsKey(tripNumber)) {
					paymentBefore = personId2tripNumber2airPollutionPayment.get(event.getPersonId()).get(tripNumber);
				}				
				double updatedPayment = paymentBefore + (-1. * event.getAmount());
				
				Map<Integer,Double> tripNumber2payment = null;
				if (personId2tripNumber2airPollutionPayment.containsKey(event.getPersonId())) {
					tripNumber2payment = personId2tripNumber2airPollutionPayment.get(event.getPersonId());
				} else {
					tripNumber2payment = new HashMap<>();
				}
				
				tripNumber2payment.put(tripNumber, updatedPayment);
				personId2tripNumber2airPollutionPayment.put(event.getPersonId(), tripNumber2payment);
				
			} else {
				log.warn("Unknown person money link description. Aborting...");
			}
			
		}
		
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2congestionPayment() {
		return personId2tripNumber2congestionPayment;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2noisePayment() {
		return personId2tripNumber2noisePayment;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2airPollutionPayment() {
		return personId2tripNumber2airPollutionPayment;
	}

}

