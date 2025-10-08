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
package org.matsim.contrib.perceivedsafety;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.bicycle.BicycleParams;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author simei94
 */
public class PerceivedSafetyAndBicycleDisutilityFactory implements TravelDisutilityFactory {
	// public-final is ok since ctor is package-private: can only be used through injection
	private static final Logger log = LogManager.getLogger(PerceivedSafetyAndBicycleDisutilityFactory.class);

	private final String mode;
	@Inject
	Scenario scenario;
	@Inject
	BicycleParams bicycleParams;
	private static int normalisationWrnCnt = 0;

	/* package-private */PerceivedSafetyAndBicycleDisutilityFactory(String mode) {
		this.mode = mode;
	}
	/* package-private */PerceivedSafetyAndBicycleDisutilityFactory() {
		this.mode = TransportMode.car;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		RoutingConfigGroup routingConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), RoutingConfigGroup.class);

		final RandomizingTimeDistanceTravelDisutilityFactory defaultTravelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(mode, scenario.getConfig());

		double sigma = routingConfigGroup.getRoutingRandomness();

		double normalization = 1;
		if ( sigma != 0. ) {
			normalization = 1. / Math.exp(sigma * sigma / 2);
			if (normalisationWrnCnt < 10) {
				normalisationWrnCnt++;
				log.info(" sigma: {}; resulting normalization: {}", sigma, normalization);
			}
		}

		return new PerceivedSafetyAndBicycleDisutility(scenario, defaultTravelDisutilityFactory.createTravelDisutility(timeCalculator), sigma,
			bicycleParams, normalization);
	}
}
