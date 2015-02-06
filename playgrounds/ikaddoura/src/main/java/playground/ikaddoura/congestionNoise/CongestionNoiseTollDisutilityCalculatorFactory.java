/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.congestionNoise;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.noise.NoiseTollHandler;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * @author ikaddoura
 *
 */
public class CongestionNoiseTollDisutilityCalculatorFactory implements TravelDisutilityFactory {

	private TollHandler congestionTollHandler;
	private NoiseTollHandler noiseTollHandler;

	public CongestionNoiseTollDisutilityCalculatorFactory(TollHandler congestionTollHandler, NoiseTollHandler noiseTollHandler) {
		this.congestionTollHandler = congestionTollHandler;
		this.noiseTollHandler = noiseTollHandler;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final CongestionNoiseTollTravelDisutilityCalculator ttdc = new CongestionNoiseTollTravelDisutilityCalculator(timeCalculator, cnScoringGroup, congestionTollHandler, noiseTollHandler);

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
