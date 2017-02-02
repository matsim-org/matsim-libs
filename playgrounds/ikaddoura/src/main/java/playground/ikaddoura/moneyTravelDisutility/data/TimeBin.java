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

package playground.ikaddoura.moneyTravelDisutility.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

/**
* Stores vehicle-specific monetary amounts for each time bin.
*  
* @author ikaddoura
*/

public class TimeBin {

	@Inject
	private AgentFilter agentFilter;
	
	private int timeBinNr;
	
	private final Map<Id<Person>, List<Double>> personId2amounts;
	
	private double averageAmount = 0.;
	private final Map<String, Double> agentType2avgAmount = new HashMap<>();
		
	public TimeBin(int timeBinNr) {
		this.timeBinNr = timeBinNr;
		this.personId2amounts = new HashMap<>();
	}

	public double getTimeBinNr() {
		return timeBinNr;
	}

	public Map<Id<Person>, List<Double>> getPersonId2amounts() {
		return personId2amounts;
	}

	public double getAverageAmount() {
		return averageAmount;
	}

	public Map<String, Double> getAgentTypeId2avgAmount() {
		return agentType2avgAmount;
	}
	
	public void computeAverageAmount() {
		double sum = 0.;
		int counter = 0;
		for (Id<Person> personId : personId2amounts.keySet()) {
			for (Double amount : personId2amounts.get(personId)) {
				sum += amount;
			}
			counter++;
		}
		
		this.averageAmount = sum / counter;
	}
	
	public void computeAverageAmountPerPersonType() {
		final Map<String, Double> agentTypeIdPrefix2AmountSum = new HashMap<>();
		final Map<String, Integer> agentTypeIdPrefix2Counter = new HashMap<>();
		
		for (Id<Person> personId : personId2amounts.keySet()) {
			double totalAmountOfPerson = 0.;
			for (Double amount : personId2amounts.get(personId)) {
				totalAmountOfPerson += amount;
			}
			
			String agentType = this.agentFilter.getAgentTypeFromId(personId);
			
			if (agentTypeIdPrefix2AmountSum.containsKey(agentType)) {
				double amountSum = agentTypeIdPrefix2AmountSum.get(agentType);
				int counter = agentTypeIdPrefix2Counter.get(agentType);
				agentTypeIdPrefix2AmountSum.put(agentType, amountSum + totalAmountOfPerson);
				agentTypeIdPrefix2Counter.put(agentType, counter + 1);

			} else {
				agentTypeIdPrefix2AmountSum.put(agentType, totalAmountOfPerson);
				agentTypeIdPrefix2Counter.put(agentType, 1);
			}
		}
		
		for (String agentType : agentTypeIdPrefix2AmountSum.keySet()) {
			this.agentType2avgAmount.put(agentType, agentTypeIdPrefix2AmountSum.get(agentType) / agentTypeIdPrefix2Counter.get(agentType) );
		}
	}

}

