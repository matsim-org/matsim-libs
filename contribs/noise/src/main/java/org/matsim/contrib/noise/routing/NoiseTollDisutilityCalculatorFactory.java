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

/**
 * 
 */
package org.matsim.contrib.noise.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */
public class NoiseTollDisutilityCalculatorFactory implements TravelDisutilityFactory {

	private NoiseContext noiseContext;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public NoiseTollDisutilityCalculatorFactory(NoiseContext noiseContext, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.noiseContext = noiseContext;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		final TollTravelDisutilityCalculator ttdc = new TollTravelDisutilityCalculator(timeCalculator, cnScoringGroup, noiseContext);

		return new TravelDisutility(){

			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double linkTravelDisutility = ttdc.getLinkTravelDisutility(link, time, person, vehicle);
				return linkTravelDisutility;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return ttdc.getLinkMinimumTravelDisutility(link);
			}
		};
	}

}
