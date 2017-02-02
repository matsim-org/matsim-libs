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

package playground.ikaddoura.moneyTravelDisutility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.moneyTravelDisutility.data.LinkInfo;
import playground.ikaddoura.moneyTravelDisutility.data.TimeBin;

/**
 * 
 * TODO: Add a blendfactor?
 * TODO: Add MSA?
 * 
* @author ikaddoura
*/

public class MoneyTimeDistanceTravelDisutility implements TravelDisutility {
	
	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final double sigma;	
	private Scenario scenario;
	private MoneyEventAnalysis moneyEventAnalysis;
	private AgentFilter vehicleFilter;
	
	public MoneyTimeDistanceTravelDisutility(
			TravelDisutility randomizedTimeDistanceTravelDisutility,
			double sigma,
			Scenario scenario,
			MoneyEventAnalysis moneyEventHandler,
			AgentFilter vehicleFilter) {

		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.sigma = sigma;
		this.scenario = scenario;
		this.moneyEventAnalysis = moneyEventHandler;
		this.vehicleFilter = vehicleFilter;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				
		double randomizedTimeDistanceDisutilityForLink = this.randomizedTimeDistanceTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
				
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person, vehicle);
		double randomizedTollDisutility = linkExpectedTollDisutility * logNormalRnd;
		
		return randomizedTimeDistanceDisutilityForLink + randomizedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	private double calculateExpectedTollDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */
				
		int intervalNr = getIntervalNr(time);
		
		double estimatedAmount = 0.;

		if (moneyEventAnalysis.getLinkId2info().containsKey(link.getId())) {
			
			LinkInfo linkInfo = moneyEventAnalysis.getLinkId2info().get(link.getId());
			
			if (linkInfo.getTimeBinNr2timeBin().containsKey(intervalNr)) {
				
				TimeBin timeBin = linkInfo.getTimeBinNr2timeBin().get(intervalNr);
				String agentType = vehicleFilter.getAgentTypeFromId(person.getId());

				double avgMoneyAmountVehicleType = 0.;
				if (timeBin.getAgentTypeId2avgAmount().containsKey(agentType)) {
					avgMoneyAmountVehicleType = timeBin.getAgentTypeId2avgAmount().get(agentType);
				}
				
				if (avgMoneyAmountVehicleType != 0.) {
					estimatedAmount = avgMoneyAmountVehicleType;
				} else {
					estimatedAmount = timeBin.getAverageAmount();;
				}
			}
		}
						
		double linkExpectedTollDisutility = -1 * this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney() * estimatedAmount;
		return linkExpectedTollDisutility;
	}
	
	private int getIntervalNr(double time) {
		
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		int timeBinNr = (int) (time / timeBinSize);
		
		return timeBinNr;
	}
	
}

