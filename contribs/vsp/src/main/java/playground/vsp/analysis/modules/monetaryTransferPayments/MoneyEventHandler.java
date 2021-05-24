/* *********************************************************************** *
 * project: org.matsim.*
 * Trb09Analysis
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
package playground.vsp.analysis.modules.monetaryTransferPayments;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * @author ikaddoura, benjamin
 *
 */
public class MoneyEventHandler implements PersonMoneyEventHandler {
	private static final Logger log = Logger.getLogger(MoneyEventHandler.class);

	private SortedMap<Id<Person>, Double> id2amount = new TreeMap<Id<Person>, Double>();
	private Set<Id<Person>> stuckingAgents = null;

	public MoneyEventHandler(Set<Id<Person>> stuckingAgents) {
		this.stuckingAgents = stuckingAgents;
		log.info("Providing the person Ids of stucking agents. These agents will be excluded from this analysis.");
	}
	
	public MoneyEventHandler() {
		log.info("Considering all persons even though they may stuck in the base case or policy case.");
	}
	
	private boolean isStucking(Id<Person> personId) {
		if (this.stuckingAgents == null) {
			return false;
		} else if (this.stuckingAgents.contains(personId)) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {

		if (isStucking(event.getPersonId())) {
			// ignore this person
			
		} else {
			Id<Person> id = event.getPersonId();
			Double amountByEvent = event.getAmount();
			Double amountSoFar = id2amount.get(id);
			
			if(amountSoFar == null){
				id2amount.put(id, amountByEvent);
			}
			else{
				amountSoFar += amountByEvent;
				id2amount.put(id, amountSoFar);
			}
		}	
	}

	public Map<Id<Person>, Double> getPersonId2amount() {
		return Collections.unmodifiableMap(id2amount);
	}
	
	public Double getSumOfMonetaryAmounts(){
		double sum = 0.;
		for (Double amount : this.id2amount.values()){
			sum = sum + amount;
		}
		return sum;
	}
	
	@Override
	public void reset(int iteration) {
		this.id2amount.clear();
	}
}
