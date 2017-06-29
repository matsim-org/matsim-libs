/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.trafficmonitoring;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.*;
import com.google.inject.name.*;

/**
 * Travel times recorded during the previous iteration. They are always updated after the mobsim ends. This is the
 * standard approach for running DVRP
 */
public class DvrpTravelTimeModule extends AbstractModule {
	public static final String DVRP_INITIAL = "dvrp_initial";
	public static final String DVRP_OBSERVED = "dvrp_observed";
	public static final String DVRP_ESTIMATED = "dvrp_estimated";

	public void install() {
		bind(TravelTime.class).annotatedWith(Names.named(DvrpTravelTimeModule.DVRP_INITIAL))
				.toInstance(new FreeSpeedTravelTime());
		bind(DvrpTravelTimeEstimator.class).to(DvrpTravelTimeEstimatorImpl.class).asEagerSingleton();
		addTravelTimeBinding(DVRP_ESTIMATED).to(DvrpTravelTimeEstimator.class);
		addMobsimListenerBinding().to(DvrpTravelTimeEstimator.class);
	}

	@Provides
	@Named(DvrpTravelTimeModule.DVRP_OBSERVED)
	@Singleton
	TravelTime provideTravelTime(@Named(TransportMode.car) TravelTime observedTT) {
		return observedTT;
	}
}
