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
package playground.benjamin.scenarios.munich.exposure;

import java.util.Map;

import org.apache.log4j.Logger;
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


/**
 * @author benjamin
 *
 */
public class EmissionResponsibilityTravelDisutilityCalculator implements TravelDisutility {
	private static final Logger logger = Logger.getLogger(EmissionResponsibilityTravelDisutilityCalculator.class);

	TravelTime timeCalculator;
	double marginalUtlOfMoney;
	double distanceCostRateCar;
	double marginalUtlOfTravelTime;
	EmissionModule emissionModule;
	EmissionResponsibilityCostModule emissionResponsibilityCostModule;


	public EmissionResponsibilityTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule, EmissionResponsibilityCostModule emissionResponsibilityCostModule) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.emissionModule = emissionModule;
		this.emissionResponsibilityCostModule = emissionResponsibilityCostModule;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {
		double linkTravelDisutility;

		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, v);
		double linkTravelTimeDisutility = this.marginalUtlOfTravelTime * linkTravelTime ;

		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double linkDistanceDisutility = this.marginalUtlOfMoney * distanceCost;

		double linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(person, link, distance, linkTravelTime, time);
		linkTravelDisutility = linkTravelTimeDisutility + linkDistanceDisutility + linkExpectedEmissionDisutility;

//		logger.info("expected emission disutility " + linkExpectedEmissionDisutility);
		return linkTravelDisutility;
	}

	private double calculateExpectedEmissionDisutility(Person person, Link link, double distance, double linkTravelTime, double time) {
		double linkExpectedEmissionDisutility;

		/* The following is an estimate of the warm emission costs that an agent (depending on her vehicle type and
		the average travel time on that link in the last iteration) would have to pay if chosing that link in the next
		iteration. Cold emission costs are assumed not to change routing; they might change mode choice or
		location choice (not implemented)! */

		Vehicle vehicle = this.emissionModule.getEmissionVehicles().getVehicles().get(person.getId());
		VehicleType vehicleType = vehicle.getType();
		String vehicleInformation = vehicleType.getId().toString();
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionHandler().getWarmEmissionAnalysisModule();
		Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
				person.getId(),
				Integer.parseInt(((LinkImpl) link).getType()),
				link.getFreespeed(),
				distance,
				linkTravelTime,
				vehicleInformation
				);
		double expectedEmissionCosts = this.emissionResponsibilityCostModule.calculateWarmEmissionCosts(expectedWarmEmissions, link.getId(), time);
		linkExpectedEmissionDisutility = this.marginalUtlOfMoney * expectedEmissionCosts ;
		// logger.info("expected emission costs for person " + person.getId() + " on link " + link.getId() + " at time " + time + " are calculated to " + expectedEmissionCosts);

		return linkExpectedEmissionDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		throw new UnsupportedOperationException();
	}
}