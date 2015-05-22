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
package playground.ikaddoura.optimization.users;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;


/**
 * @author Ihab
 *
 */
public class MoneyEventHandler implements PersonMoneyEventHandler {

	private double revenues;
	private List<FareData> fareDataList = new ArrayList<FareData>();
	private Map<Id<Person>, List<Double>> person2amounts = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		this.revenues = 0;
		this.fareDataList.clear();
		this.person2amounts.clear();
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		
		this.revenues = this.revenues + (-1 * event.getAmount());
		
		FareData fareData = new FareData();
		fareData.setAmount(-1 * event.getAmount());
		fareData.setTime(event.getTime());
		fareData.setPersonId(event.getPersonId());
		this.fareDataList.add(fareData);
		
		if (person2amounts.containsKey(event.getPersonId())){
			this.person2amounts.get(event.getPersonId()).add(event.getAmount());
		
		} else {
			List<Double> amounts = new ArrayList<Double>();
			amounts.add(event.getAmount());
			this.person2amounts.put(event.getPersonId(), amounts);
		}
	}

	/**
	 * @return the revenues
	 */
	public double getRevenues() {
		return revenues;
	}

	public List<FareData> getfareDataList() {
		return fareDataList;
	}
	
	public double getAverageAmountPerPerson() {
		
		double averageAmountSum = 0.;
		int personCounter = 0;
		for (Id<Person> personId : this.person2amounts.keySet()){			
			double amountSum = 0.;
			int counter = 0;
			for (Double amount : this.person2amounts.get(personId)){
				amountSum = amountSum + amount;				
				counter++;
			}
			double averageAmount = amountSum / counter;
			averageAmountSum = averageAmountSum + averageAmount;
			personCounter++;
		}
		double avgAmountPerAgent = -1 * (averageAmountSum / personCounter);
		return avgAmountPerAgent;
	}
	
}
