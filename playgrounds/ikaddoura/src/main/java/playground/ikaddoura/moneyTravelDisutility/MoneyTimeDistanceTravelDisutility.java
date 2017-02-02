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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * TODO: Add something like a blendfactor...
 * TODO: Add something like MSA...
 * 
* @author ikaddoura
*/

public class MoneyTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger(MoneyTimeDistanceTravelDisutility.class);

	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final double sigma;	
	private Scenario scenario;
	private MoneyEventAnalysis moneyEventAnalysis;
		
	public MoneyTimeDistanceTravelDisutility(
			TravelDisutility randomizedTimeDistanceTravelDisutility,
			double sigma,
			Scenario scenario,
			MoneyEventAnalysis moneyEventHandler) {

		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.sigma = sigma;
		this.scenario = scenario;
		this.moneyEventAnalysis = moneyEventHandler;
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
		
		// TODO: identify vehicle type based on vehicles file or vehicle ID
		
		int intervalNr = getIntervalNr(time);
		
		double avgMoneyAmountPerVehcile = 0.;
		
		if (moneyEventAnalysis.getLinkId2info().containsKey(link.getId())) {
			if (moneyEventAnalysis.getLinkId2info().get(link.getId()).getTimeBinNr2timeBin().containsKey(intervalNr)) {
				avgMoneyAmountPerVehcile = moneyEventAnalysis.getLinkId2info().get(link.getId()).getTimeBinNr2timeBin().get(intervalNr).getAverageAmount();
				log.warn("avg money amount: " + avgMoneyAmountPerVehcile);
			}
		}
				
		double linkExpectedTollDisutility = -1 * this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney() * avgMoneyAmountPerVehcile;
		return linkExpectedTollDisutility;
	}
	
	private int getIntervalNr(double time) {
		
		double timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		int timeBinNr = (int) (time / timeBinSize);
		
		return timeBinNr;
	}
	
}

