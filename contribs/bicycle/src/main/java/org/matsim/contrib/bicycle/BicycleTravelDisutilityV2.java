/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

/**
 * @author smetzler, dziemke
 */
public class BicycleTravelDisutilityV2 implements TravelDisutility {
	private static final Logger LOG = Logger.getLogger(BicycleTravelDisutilityV2.class);

	private final double marginalCostOfInfrastructure_m;
	private final double marginalCostOfComfort_m;
	private final double marginalCostOfGradient_m_100m;

	private final TravelDisutility timeDistanceDisutility;
	

	BicycleTravelDisutilityV2(TravelDisutility timeDistanceDisutility, BicycleConfigGroup bicycleConfigGroup, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.timeDistanceDisutility = timeDistanceDisutility;
		
		final PlanCalcScoreConfigGroup.ModeParams bicycleParams = cnScoringGroup.getModes().get("bicycle");
		if (bicycleParams == null) {
			throw new NullPointerException("Bicycle is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
		}

		this.marginalCostOfInfrastructure_m = -(bicycleConfigGroup.getMarginalUtilityOfInfrastructure_m());
		this.marginalCostOfComfort_m = -(bicycleConfigGroup.getMarginalUtilityOfComfort_m());
		this.marginalCostOfGradient_m_100m = -(bicycleConfigGroup.getMarginalUtilityOfGradient_m_100m());
	}

	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		String surface = (String) link.getAttributes().getAttribute(BicycleLabels.SURFACE);
		String type = (String) link.getAttributes().getAttribute("type");
		String cyclewaytype = (String) link.getAttributes().getAttribute(BicycleLabels.CYCLEWAY);

		double distance = link.getLength();
		
		double comfortFactor = BicycleUtilityUtils.getComfortFactor(surface, type);
		double comfortDisutility = marginalCostOfComfort_m * (1. - comfortFactor) * distance;
		
		double infrastructureFactor = BicycleUtilityUtils.getInfrastructureFactor(type, cyclewaytype);
		double infrastructureDisutility = marginalCostOfInfrastructure_m * (1. - infrastructureFactor) * distance;
		
		double gradientFactor = BicycleUtilityUtils.getGradientFactor(link);
		double gradientDisutility = marginalCostOfGradient_m_100m * gradientFactor * distance;
		
//		LOG.warn("link = " + link.getId() + "-- travelTime = " + travelTime + " -- distance = " + distance + " -- comfortFactor = "
//				+ comfortFactor	+ " -- infraFactor = "+ infrastructureFactor + " -- gradient = " + gradientFactor);
		 
		// TODO Gender
		// TODO Activity
		// TODO Other influence factors
		
		double disutility = timeDistanceDisutility.getLinkTravelDisutility(link, time, person, vehicle) + infrastructureDisutility + comfortDisutility + gradientDisutility;
		LOG.warn("---------- disutility = " + disutility);
		return disutility;
	}

	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}
}