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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;

public class ParkingPersonalBetas {

	private final Scenario scenario;
	private final HashMap<Id, Double> houseHoldIncome;
	public double externalWalkFactor=1.0;
	public double externalSearchFactor=1.0;

	public ParkingPersonalBetas(Scenario scenario, HashMap<Id, Double> houseHoldIncome) {
		this.scenario = scenario;

		if (houseHoldIncome == null) {
			houseHoldIncome = new HashMap<Id, Double>();

			for (Id personId : scenario.getPopulation().getPersons().keySet()) {
				if (MatsimRandom.getRandom().nextBoolean()) {
					houseHoldIncome.put(personId, 4000.0);
				} else {
					houseHoldIncome.put(personId, 8000.0);

				}
			}
		}

		this.houseHoldIncome = houseHoldIncome;
	}

	public double getParkingCostBeta(Id personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		// person.getSex();

		double income = houseHoldIncome.get(personId);
		//return -1.0* -0.135 * (-0.1) / -0.056 * -0.135 * Math.pow((income / 7000), -0.1);
		return -0.135 *60* Math.pow((income / 7000), -0.1);
	}

	public double getParkingSearchTimeBeta(Id personId, double activityDurationInSeconds) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		
		int isMale=1;
		if (PersonUtils.getSex(person)!=null){
			isMale=!PersonUtils.getSex(person).contains("f")?1:0;
		}
		//return -1.0*-0.135 * (-0.1) / -0.056 * -0.135 * Math.pow(activityDurationInSeconds / 60 / 135, -0.246)*(1+(-0.1012*isMale));
		return -0.135 * 60 * Math.pow(activityDurationInSeconds / 60 / 135, -0.246)*(1+(-0.102*isMale))*externalSearchFactor;
	}

	public double getParkingWalkTimeBeta(Id personId, double activityDurationInSeconds) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		
		int isMale=1;
		if (PersonUtils.getSex(person)!=null){
			isMale=!PersonUtils.getSex(person).contains("f")?1:0;
		}
		int age= PersonUtils.getAge(person);
		//return -1.0*-0.108 * (-0.1) / -0.056 * -0.108*60 * Math.pow(activityDurationInSeconds / 60 / 135, -0.08)*(1+(0.021*isMale))*Math.pow(age / 40.0, 0.236);
		return -0.108 *60 * Math.pow(activityDurationInSeconds / 60 / 135, -0.08)*(1+(0.021*isMale))*Math.pow(age / 40.0, 0.236)*externalWalkFactor;
	}

}
