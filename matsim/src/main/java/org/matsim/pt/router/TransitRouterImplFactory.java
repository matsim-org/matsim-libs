/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterImplFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * @author mrieser
 */
@Singleton
public class TransitRouterImplFactory implements Provider<TransitRouter> {

	private final TransitRouterConfig config;
	private final TransitSchedule transitSchedule;
	private TransitRouterNetwork routerNetwork;
	private PreparedTransitSchedule preparedTransitSchedule;

	@Inject
	TransitRouterImplFactory(final TransitSchedule schedule, final EventsManager events, final Config config) {
		this(schedule, new TransitRouterConfig(
				config.planCalcScore(),
				config.plansCalcRoute(),
				config.transitRouter(),
				config.vspExperimental()));
		events.addHandler((TransitScheduleChangedEventHandler) event -> {
			routerNetwork = null;
			preparedTransitSchedule = null;
		});
	}

	public TransitRouterImplFactory(final TransitSchedule schedule, final TransitRouterConfig config) {
		this.config = config;
		this.transitSchedule = schedule;
	}

	@Override
	public TransitRouter get() {
		if (this.routerNetwork == null) {
			this.routerNetwork = TransitRouterNetwork.createFromSchedule(transitSchedule, this.config.getBeelineWalkConnectionDistance());
		}
		if (this.preparedTransitSchedule == null) {
			this.preparedTransitSchedule = new PreparedTransitSchedule(transitSchedule);
		}

		TransitRouterNetworkTravelTimeAndDisutility ttCalculator = new TransitRouterNetworkTravelTimeAndDisutility(this.config, this.preparedTransitSchedule);
		return new TransitRouterImpl(this.config, this.preparedTransitSchedule, this.routerNetwork, ttCalculator, ttCalculator);
	}
	
}
