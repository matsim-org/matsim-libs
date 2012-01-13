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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import playground.benjamin.emissions.EmissionModule;
import playground.benjamin.emissions.WarmEmissionAnalysisModule;
import playground.benjamin.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionTravelCostCalculator implements PersonalizableTravelCost{
	
	TravelTime timeCalculator;
	double marginalUtlOfMoney;
	double distanceCostRateCar;
	double marginalUtlOfTravelTime;
	Person person;
	EmissionModule emissionModule;
	EmissionCostModule costModule = new EmissionCostModule();

	public EmissionTravelCostCalculator(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule) {
		this.timeCalculator = timeCalculator;
		this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
		this.distanceCostRateCar = cnScoringGroup.getMonetaryDistanceCostRateCar();
		this.marginalUtlOfTravelTime = (- cnScoringGroup.getTraveling_utils_hr() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
		this.emissionModule = emissionModule;
	}

	@Override
	public void setPerson(Person person) {
		this.person = person;
	}

	@Override
	public double getLinkGeneralizedTravelCost(Link link, double time) {
		double generalizedTravelCost;
		
		double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time);
		double generalizedTravelTimeCost = this.marginalUtlOfTravelTime * linkTravelTime ;
		
		double distance = link.getLength();
		double distanceCost = - this.distanceCostRateCar * distance;
		double generalizedDistanceCost = this.marginalUtlOfMoney * distanceCost;
		
		Vehicle vehicle = this.emissionModule.getEmissionVehicles().getVehicles().get(person.getId());
		VehicleType vehicleType = vehicle.getType();
		String vehicleInformation = vehicleType.getId().toString();
		WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionsHandler().getWarmEmissionAnalysisModule();
		Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
				person.getId(),
				Integer.parseInt(((LinkImpl) link).getType()),
				link.getFreespeed(),
				distance,
				linkTravelTime,
				vehicleInformation
				);
		// cold emission costs are assumed not to change routing; they might change mode choice (not implemented)!
		double expectedEmissionCosts = costModule.calculateWarmEmissionCosts(expectedWarmEmissions );
		double generalizedExpectedEmissionCost = this.marginalUtlOfMoney * expectedEmissionCosts ;
		
		if(link.getId().equals(new IdImpl("11"))){
			generalizedTravelCost = generalizedTravelTimeCost + generalizedDistanceCost + generalizedExpectedEmissionCost;
		} else {
			generalizedTravelCost = generalizedTravelTimeCost + generalizedDistanceCost;
		}
		return generalizedTravelCost;
	}

}
