/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.benchmark;

import javax.inject.Named;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 */
class DvrpBenchmarkQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(QNetworkFactory.class).toProvider(new Provider<>() {
			@Inject
			private Scenario scenario;

			@Inject
			private EventsManager events;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime estimatedTravelTime;

			@Override
			public QNetworkFactory get() {
				ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory(events, scenario);
				factory.setLinkSpeedCalculator(
						(vehicle, link, time) -> calcLinkSpeed(estimatedTravelTime, vehicle.getVehicle(), link, time));
				return factory;
			}
		});
	}

	static double calcLinkSpeed(TravelTime travelTime, Vehicle vehicle, Link link, double time) {
		return link.getLength() / travelTime.getLinkTravelTime(link, time, null, vehicle);
	}
}
