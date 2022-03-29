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

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;

import java.util.Map;

import static org.matsim.core.config.ConfigUtils.addOrGetModule;

/**
* @author ikaddoura
*/
public final class NoiseDefaultCarTravelDisutilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(NoiseDefaultCarTravelDisutilityModule.class);

	@Override
	public void install() {
		
		NoiseConfigGroup noiseParameters = addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
				
		if (noiseParameters.isInternalizeNoiseDamages()) {
			
			log.info("Replacing the default travel disutility for the transport mode 'car' by a travel distuility which accounts for noise tolls.");
			
			if (!noiseParameters.isComputeAvgNoiseCostPerLinkAndTime()) {
				log.warn("The travel disutility which accounts for noise tolls requires the computation of average noise cost per link and time bin."
						+ "Setting the value 'computeAvgNoiseCostPerLinkAndTime' to 'true'...");
				noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(true);
			}

			AbstractModule module = new AbstractModule(){
				@Override public void install(){
					install( new TravelDisutilityModule() ) ;
				}
			};
			com.google.inject.Injector injector = Injector.createInjector( this.getConfig() , module );
			Map<String,TravelDisutilityFactory> factories = injector.getInstance( Key.get( new TypeLiteral<Map<String, TravelDisutilityFactory>>(){} ) ) ;
			TravelDisutilityFactory defaultFactoryForCar = factories.get( TransportMode.car ) ;
			// yy should rather insert the above, but I think that for the time being the default variant is not even using the randomizing version
			// .  Kai, aug'19
			// probably now fixed.  kai, mar'20

			final NoiseTollTimeDistanceTravelDisutilityFactory tollDisutilityCalculatorFactory = new NoiseTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, this.getConfig())
			);
			bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
		}		
	}
}

