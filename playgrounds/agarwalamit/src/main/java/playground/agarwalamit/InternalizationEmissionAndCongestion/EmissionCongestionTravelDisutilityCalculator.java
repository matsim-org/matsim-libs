/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.InternalizationEmissionAndCongestion;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.benjamin.internalization.EmissionCostModule;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author amit after Banjamin and Ihab
 */
public class EmissionCongestionTravelDisutilityCalculator implements TravelDisutility{

	private final Logger logger = Logger.getLogger(EmissionCongestionTravelDisutilityCalculator.class);
	
	/*
	 * Blur the Social Cost to speed up the relaxation process. Values between
	 * 0.0 and 1.0 are valid. 0.0 means the old value will be kept, 1.0 means
	 * the old value will be totally overwritten.
	 */
	private final double blendFactor = 0.1;
	private TravelTime timeCalculator;
	private double marginalUtlOfMoney;
	private double distanceCostRateCar;
	private double marginalUtlOfTravelTime;
	private EmissionModule emissionModule;
	private EmissionCostModule emissionCostModule;
	private final Set<Id<Link>> hotspotLinks;
	private TollHandler tollHandler;


	public EmissionCongestionTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule, EmissionCostModule emissionCostModule, Set<Id<Link>> hotspotLinks, TollHandler tollHandler) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
		this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.hotspotLinks = hotspotLinks;
		
		this.tollHandler = tollHandler;
		this.logger.info("The 'blend factor' which is used for the calculation of the expected tolls in the next iteration is set to " + this.blendFactor);
		
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {
		Vehicle emissionVehicle = v;
		if (v == null){
			// the link travel disutility is asked without information about the vehicle
			if (person == null){
				// additionally, no person is given -> a default vehicle type is used
				Log.warn("No person and no vehicle is given to calculate the link travel disutility. The default vehicle type is used to estimate emission disutility.");
				emissionVehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("defaultVehicle"), VehicleUtils.getDefaultVehicleType());
			} else {
				// a person is given -> use the vehicle for that person given in emissionModule
				emissionVehicle = this.emissionModule.getEmissionVehicles().getVehicles().get(person.getId());
			}
		}
		
		double linkTravelDisutility;

		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, emissionVehicle);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		double linkExpectedEmissionDisutility;

		if(this.hotspotLinks == null){
			// pricing applies for all links
			linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, distance, linkTravelTime);
		} else {
			// pricing applies for the current link
			if(this.hotspotLinks.contains(link.getId())) linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, distance, linkTravelTime);
			// pricing applies not for the current link
			else linkExpectedEmissionDisutility = 0.0;
		}
		/* // Test the routing:
			if(!link.getId().equals(new IdImpl("11"))) 
			generalizedTravelCost = generalizedTravelTimeCost + generalizedDistanceCost;
			else */	
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person);
		linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + linkExpectedEmissionDisutility + linkExpectedTollDisutility;

			return linkTravelDisutility;
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
				Integer.parseInt(((LinkImpl) link).getType()),
				link.getFreespeed(),
				distance,
				linkTravelTime
				);
		double expectedEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(expectedWarmEmissions);
		linkExpectedEmissionDisutility = this.marginalUtlOfMoney * expectedEmissionCosts ;
		// logger.info("expected emission costs for person " + person.getId() + " on link " + link.getId() + " at time " + time + " are calculated to " + expectedEmissionCosts);

		return linkExpectedEmissionDisutility;
	}
	
	private double calculateExpectedTollDisutility(Link link, double time, Person person) {

		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 and i-2 */
		
		double linkExpectedTollNewValue = this.tollHandler.getAvgToll(link.getId(), time);
		double linkExpectedTollOldValue = this.tollHandler.getAvgTollOldValue(link.getId(), time);

		double blendedOldValue = (1 - this.blendFactor) * linkExpectedTollOldValue;
		double blendedNewValue = this.blendFactor * linkExpectedTollNewValue;	
		
//		if (linkExpectedTollNewValue != 0 || linkExpectedTollOldValue != 0) {
//			log.info("-----------> Person " + person.getId() + ": Expected toll (new value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollNewValue);
//			log.info("-----------> Person " + person.getId() + ": Expected toll (old value) on link " + link.getId() + " at " + Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + ": " + linkExpectedTollOldValue);
//		
//			log.info("ExpectedToll: " + (blendedNewValue + blendedOldValue) );
//		}
				
		double linkExpectedTollDisutility = -1 * this.marginalUtlOfMoney * (blendedOldValue + blendedNewValue);			
		return linkExpectedTollDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
}
