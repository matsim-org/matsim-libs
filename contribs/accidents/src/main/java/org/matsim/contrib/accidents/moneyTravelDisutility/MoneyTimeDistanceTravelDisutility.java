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

package org.matsim.contrib.accidents.moneyTravelDisutility;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
* 
* A travel disutility which adds a term for monetary payments to the disutility which is passed into the constructor.
* The monetary payments are estimated based on time, link- and agent-specific {@link PersonLinkMoneyEvent}s in the previous iteration(s)
* that are caught and analyzed by {@link MoneyEventAnalysis}.
* 
* @author ikaddoura
*/

public class MoneyTimeDistanceTravelDisutility implements TravelDisutility {

	private final TravelDisutility travelDisutility;
	private final double sigma;	
	private final double timeBinSize;
	private final double marginalUtilityOfMoney;
	private MoneyEventAnalysis moneyEventAnalysis;
	private AgentFilter vehicleFilter;
	
	public MoneyTimeDistanceTravelDisutility(
			TravelDisutility travelDisutility,
			double sigma,
			Scenario scenario,
			MoneyEventAnalysis moneyEventHandler,
			AgentFilter vehicleFilter) {

		this.travelDisutility = travelDisutility;
		this.sigma = sigma;
		this.moneyEventAnalysis = moneyEventHandler;
		this.vehicleFilter = vehicleFilter;
		this.timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
	}
	
	public MoneyTimeDistanceTravelDisutility(
			TravelDisutility travelDisutility,
			Scenario scenario,
			MoneyEventAnalysis moneyEventHandler) {

		this.travelDisutility = travelDisutility;
		this.sigma = 0.;
		this.moneyEventAnalysis = moneyEventHandler;
		this.vehicleFilter = null;
		this.timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {

		double travelDisutility = 0.;
		
		if (this.travelDisutility != null) {
			travelDisutility = this.travelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		}
		
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
				
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person, vehicle);
		double randomizedTollDisutility = linkExpectedTollDisutility * logNormalRnd;
				
		return travelDisutility + randomizedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		
		if (this.travelDisutility != null) {
			return this.travelDisutility.getLinkMinimumTravelDisutility(link);
		} else {
			return 0.;
		}
	}

	private double calculateExpectedTollDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */
				
		int intervalNr = getIntervalNr(time);
		
		double estimatedAmount = 0.;

		LinkInfo linkInfo = moneyEventAnalysis.getLinkId2info().get(link.getId());
		if (linkInfo != null) {
						
			TimeBin timeBin = linkInfo.getTimeBinNr2timeBin().get(intervalNr);
			if (timeBin != null) {

				if(this.vehicleFilter != null) {
					Id<Person> personId = null;
					if (person != null) {
						personId = person.getId();
					} else {
						// person Id is null
						System.out.println();
					}
					String agentType = vehicleFilter.getAgentTypeFromId(personId);
					Double avgMoneyAmountVehicleType = timeBin.getAgentTypeId2avgAmount().get(agentType);
					
					if (avgMoneyAmountVehicleType != null && avgMoneyAmountVehicleType != 0.) {
						estimatedAmount = avgMoneyAmountVehicleType;
					} else {
						estimatedAmount = timeBin.getAverageAmount();
					}
				} else {
					estimatedAmount = timeBin.getAverageAmount();
				}
			}
		}
				
		double linkExpectedTollDisutility = -1 * this.marginalUtilityOfMoney * estimatedAmount;
		return linkExpectedTollDisutility;
	}
	
	private int getIntervalNr(double time) {
		int timeBinNr = (int) (time / timeBinSize);
		return timeBinNr;
	}
	
}

