/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.utility;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;

public class ParkingPersonalBetas {

	private final ScenarioImpl scenario;
	private final HashMap<Id, Double> houseHoldIncome;

	public ParkingPersonalBetas(ScenarioImpl scenario, HashMap<Id, Double> houseHoldIncome) {
		this.scenario = scenario;
		
		if (this.houseHoldIncome==null){
			houseHoldIncome=new HashMap<Id, Double>();
			
			for (Id personId:scenario.getPopulation().getPersons().keySet()){
				if (MatsimRandom.getRandom().nextBoolean()){
					houseHoldIncome.put(personId, 4000.0);
				} else {
					houseHoldIncome.put(personId, 8000.0);
					
				}
			}
		}
		
		this.houseHoldIncome = houseHoldIncome;
	}

	public double getParkingCostBeta(Id personId) {
		PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get(personId);
		// person.getSex();

		double income = houseHoldIncome.get(personId);
		return -(1 / 2.77) * 0.129 * Math.pow((income / 7000), -0.089)/60;
	}

	public double getParkingSearchTimeBeta(Id personId, double activityDurationInSeconds) {
		double income = houseHoldIncome.get(personId);
		return -(1 / 2.77) * 0.6 * getParkingCostBeta(personId) * Math.pow(activityDurationInSeconds / 60 / 135, -0.336)
				* Math.pow((income / 7000), 0.079)/60;
	}

	public double getParkingWalkTimeBeta(Id personId, double activityDurationInSeconds) {
		PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get(personId);
		// person.getAge();
		// person.getSex();
		return -(1 / 2.77) * 0.117;
	}

}
