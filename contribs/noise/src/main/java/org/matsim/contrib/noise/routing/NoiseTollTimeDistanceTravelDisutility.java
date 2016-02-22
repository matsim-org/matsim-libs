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

package org.matsim.contrib.noise.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
* @author ikaddoura
*/

public class NoiseTollTimeDistanceTravelDisutility implements TravelDisutility {
	
	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final NoiseContext noiseContext;
	private final double marginalUtilityOfMoney;
	private final double sigma ;
	
	public NoiseTollTimeDistanceTravelDisutility(TravelDisutility randomizedTimeDistanceTravelDisutility,
			NoiseContext noiseContext,
			double marginalUtilityOfMoney,
			double sigma) {

		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.noiseContext = noiseContext;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.sigma = sigma;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				
		double randomizedTimeDistanceDisutilityForLink = this.randomizedTimeDistanceTravelDisutility.getLinkTravelDisutility(link, time, person, vehicle);
		
		double logNormalRnd = 1. ;
		if ( sigma != 0. ) {
			logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
		}
				
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link.getId(), time, person.getId());
		double randomizedTollDisutility = linkExpectedTollDisutility * logNormalRnd;
		
		return randomizedTimeDistanceDisutilityForLink + randomizedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}

	private double calculateExpectedTollDisutility(Id<Link> linkId, double time, Id<Person> personId) {
		
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */
		
		double linkExpectedToll = 0.;
		double timeIntervalEndTime = ((int) (time / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) + 1) * this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		
		if (this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime) == null ||
				this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId) == null) {
				// expected toll on that link should be zero
			
		} else {
			
			boolean isHGV = false;
			for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
				if (personId.toString().startsWith(hgvPrefix)) {
					isHGV = true;
					break;
				}
			}
			
			if (isHGV) {
			
				if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {	
					linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getAverageDamageCostPerHgv();
					
				} else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
					linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getMarginalDamageCostPerHgv();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
				
			} else {
				
				if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {	
					linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getAverageDamageCostPerCar();
					
				} else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
					linkExpectedToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(linkId).getMarginalDamageCostPerCar();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
			}

		}
		
//		log.warn("Expected toll on link " + linkId + " at time " + time + " in time interval " + timeIntervalEndTime + ": " + linkExpectedToll);
				
		double linkExpectedTollDisutility = this.marginalUtilityOfMoney * linkExpectedToll;			
		return linkExpectedTollDisutility;
	}
	
}

