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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
* Stores vehicle-specific monetary amounts for each time bin.
*  
* @author ikaddoura
*/

public class TimeBin {
	
	@Override
	public String toString() {
		return "TimeBin [timeBinNr=" + timeBinNr + ", personId2amounts=" + personId2amounts + ", enteringAgents="
				+ enteringAgents + ", averageAmount=" + averageAmount + ", agentType2avgAmount=" + agentType2avgAmount
				+ "]";
	}

	private int timeBinNr;
	
	private final Map<Id<Person>, List<Double>> personId2amounts;
	private final List<Id<Person>> enteringAgents;
	
	private double averageAmount = 0.;
	private final Map<String, Double> agentType2avgAmount = new HashMap<>();
		
	public TimeBin(int timeBinNr) {
		this.timeBinNr = timeBinNr;
		this.personId2amounts = new HashMap<>();
		this.enteringAgents = new ArrayList<>();
	}

	public void setAverageAmount(double averageAmount) {
		this.averageAmount = averageAmount;
	}
	
	public double getAverageAmount() {
		return averageAmount;
	}
	
	public double getTimeBinNr() {
		return timeBinNr;
	}

	public Map<Id<Person>, List<Double>> getPersonId2amounts() {
		return personId2amounts;
	}
	
	public Map<String, Double> getAgentTypeId2avgAmount() {
		return agentType2avgAmount;
	}

	public List<Id<Person>> getEnteringAgents() {
		return enteringAgents;
	}

}

