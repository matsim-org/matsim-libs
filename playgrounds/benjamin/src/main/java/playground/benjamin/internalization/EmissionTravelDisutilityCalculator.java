/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionTravelCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.internalization;

import java.util.Map;
import java.util.Set;

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


/**
 * @author benjamin
 *
 */
public class EmissionTravelDisutilityCalculator implements TravelDisutility {

	TravelTime timeCalculator;
	double marginalUtlOfMoney;
	double distanceCostRateCar;
	double marginalUtlOfTravelTime;
	EmissionModule emissionModule;
	EmissionCostModule emissionCostModule;
	private final Set<Id<Link>> hotspotLinks;


	public EmissionTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule, EmissionCostModule emissionCostModule, Set<Id<Link>> hotspotLinks) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
		this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.emissionModule = emissionModule;
		this.emissionCostModule = emissionCostModule;
		this.hotspotLinks = hotspotLinks;
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

		if(hotspotLinks == null){
			// pricing applies for all links
			linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, distance, linkTravelTime);
		} else {
			// pricing applies for the current link
			if(hotspotLinks.contains(link.getId())) linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, distance, linkTravelTime);
			// pricing applies not for the current link
			else linkExpectedEmissionDisutility = 0.0;
		}
		/* // Test the routing:
			if(!link.getId().equals(new IdImpl("11"))) 
			generalizedTravelCost = generalizedTravelTimeCost + generalizedDistanceCost;
			else */	linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + linkExpectedEmissionDisutility;

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

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
}