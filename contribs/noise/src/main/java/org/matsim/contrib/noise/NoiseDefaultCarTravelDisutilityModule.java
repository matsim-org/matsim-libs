/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.noise.routing.NoiseTollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;

/**
* @author ikaddoura
*/
final class NoiseDefaultCarTravelDisutilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(NoiseDefaultCarTravelDisutilityModule.class);

	@Override
	public void install() {
		
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
				
		if (noiseParameters.isInternalizeNoiseDamages()) {
			
			log.info("Replacing the default travel disutility for the transport mode 'car' by a travel distuility which accounts for noise tolls.");
			
			if (!noiseParameters.isComputeAvgNoiseCostPerLinkAndTime()) {
				log.warn("The travel disutility which accounts for noise tolls requires the computation of average noise cost per link and time bin."
						+ "Setting the value 'computeAvgNoiseCostPerLinkAndTime' to 'true'...");
				noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(true);
			}
			
			final NoiseTollTimeDistanceTravelDisutilityFactory tollDisutilityCalculatorFactory = new NoiseTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, this.getConfig().planCalcScore()),
					this.getConfig().planCalcScore()
					);
			bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
		}		
	}
}

