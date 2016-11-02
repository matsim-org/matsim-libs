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

package playground.ikaddoura.integrationCNE;

import java.util.Map;
import java.util.Set;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import playground.benjamin.internalization.EmissionCostModule;
import playground.vsp.congestion.handlers.TollHandler;

/**
* @author ikaddoura, amit
*/

public class CNETollTimeDistanceTravelDisutility implements TravelDisutility {
	
	/*
	 * Blur the Social Cost to speed up the relaxation process. Values between
	 * 0.0 and 1.0 are valid. 0.0 means the old value will be kept, 1.0 means
	 * the old value will be totally overwritten.
	 */
	private final double blendFactor = 1.0;
	private final TravelDisutility randomizedTimeDistanceTravelDisutility;
	private final TravelTime timeCalculator;
	private final EmissionModule emissionModule;
	private final EmissionCostModule emissionCostModule;
	private final NoiseContext noiseContext;
	private TollHandler tollHandler;
	private final double marginalUtilityOfMoney;
	private final double sigma ;
	private final Set<Id<Link>> hotspotLinks;
	
	
	public CNETollTimeDistanceTravelDisutility(TravelDisutility randomizedTimeDistanceTravelDisutility,
			TravelTime timeCalculator,
			EmissionModule emissionModule, EmissionCostModule emissionCostModule,
			NoiseContext noiseContext,
			TollHandler tollHandler,
			double marginalUtilityOfMoney,
			double sigma,
			Set<Id<Link>> hotspotLinks) {

		this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
		this.timeCalculator = timeCalculator;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney;
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.noiseContext = noiseContext;
		this.tollHandler = tollHandler;
		this.sigma = sigma;
		this.hotspotLinks = hotspotLinks;
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
		
		// congestion toll distulity
		
		double linkExpectedTollNewValue = this.tollHandler.getAvgToll(link.getId(), time);
		double linkExpectedTollOldValue = this.tollHandler.getAvgTollOldValue(link.getId(), time);

		double blendedOldValue = (1 - blendFactor) * linkExpectedTollOldValue;
		double blendedNewValue = blendFactor * linkExpectedTollNewValue;	

		double expectedLinkCongestionTollDisutility = -1 * this.marginalUtilityOfMoney * (blendedOldValue + blendedNewValue);			

		// noise toll disutility
		
		double linkExpectedNoiseToll = 0.;
		double timeIntervalEndTime = ((int) (time / this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()) + 1) * this.noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
		
		if (this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime) == null ||
				this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()) == null) {
			// expected toll on that link should be zero
			
		} else {
			
			boolean isHGV = false;
			for (String hgvPrefix : this.noiseContext.getNoiseParams().getHgvIdPrefixesArray()) {
				if (person.getId().toString().startsWith(hgvPrefix)) {
					isHGV = true;
					break;
				}
			}
			
			if (isHGV) {
						
				if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {	
					linkExpectedNoiseToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getAverageDamageCostPerHgv();
					
				} else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
					linkExpectedNoiseToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getMarginalDamageCostPerHgv();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
				
			} else {
				
				if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.AverageCost) {	
					linkExpectedNoiseToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getAverageDamageCostPerCar();
					
				} else if (this.noiseContext.getNoiseParams().getNoiseAllocationApproach() == NoiseAllocationApproach.MarginalCost) {
					linkExpectedNoiseToll = this.noiseContext.getTimeInterval2linkId2noiseLinks().get(timeIntervalEndTime).get(link.getId()).getMarginalDamageCostPerCar();
					
				} else {
					throw new RuntimeException("Unknown noise allocation approach. Aborting...");
				}
			}

		}
						
		double expectedLinkNoiseTollDistuility = this.marginalUtilityOfMoney * linkExpectedNoiseToll;			
		
		// exhaust emission toll disutility
		
		Vehicle emissionVehicle = vehicle;
		if (vehicle == null){
			// the link travel disutility is asked without information about the vehicle
			if (person == null){
				// additionally, no person is given -> a default vehicle type is used
				Log.warn("No person and no vehicle is given to calculate the link travel disutility. The default vehicle type is used to estimate emission disutility.");
				emissionVehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("defaultVehicle"), VehicleUtils.getDefaultVehicleType());
			} else {
				// a person is given -> use the vehicle for that person given in emissionModule
				emissionVehicle = this.emissionModule.getEmissionVehicles().getVehicles().get(Id.createVehicleId(person.getId()));
			}
		}
		
		double expectedLinkExhaustEmissionTollDisutility;

		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, emissionVehicle);

		if(this.hotspotLinks == null){
			// pricing applies for all links
			expectedLinkExhaustEmissionTollDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime);
		} else {
			// pricing applies for the current link
			if(this.hotspotLinks.contains(link.getId())) expectedLinkExhaustEmissionTollDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime);
			// pricing applies not for the current link
			else expectedLinkExhaustEmissionTollDisutility = 0.0;
		}
		
		// congestion + noise + exhaust emissions
		
		double linkExpectedTollDisutility = expectedLinkCongestionTollDisutility + expectedLinkNoiseTollDistuility + expectedLinkExhaustEmissionTollDisutility;
		return linkExpectedTollDisutility;
	}
	
	private double calculateExpectedEmissionDisutility(Vehicle vehicle, Link link, double distance, double linkTravelTime) {
		double linkExpectedEmissionDisutility;

		/* The following is an estimate of the warm emission costs that an agent (depending on her vehicle type and
		the average travel time on that link in the last iteration) would have to pay if chosing that link in the next
		iteration. Cold emission costs are assumed not to change routing; they might change mode choice or
		location choice (not implemented)! */

		WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionHandler().getWarmEmissionAnalysisModule();
		Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
				vehicle,
				Integer.parseInt(NetworkUtils.getType(((Link) link))),
				link.getFreespeed(),
				distance,
				linkTravelTime
				);
		double expectedEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(expectedWarmEmissions);
		linkExpectedEmissionDisutility = this.marginalUtilityOfMoney * expectedEmissionCosts ;

		return linkExpectedEmissionDisutility;
	}
	
}

