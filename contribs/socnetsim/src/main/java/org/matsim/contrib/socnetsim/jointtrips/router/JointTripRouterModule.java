/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.router;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;

import javax.inject.Named;
import javax.inject.Provider;

/**
 * @author thibautd
 */
public class JointTripRouterModule extends AbstractModule {

	@Override
	public void install() {
		addRoutingModuleBinding(JointActingTypes.DRIVER).toProvider(DriverRoutingModuleProvider.class);
		addRoutingModuleBinding(JointActingTypes.PASSENGER).toProvider(PassengerRoutingModuleProvider.class);
	}

	private static class DriverRoutingModuleProvider implements Provider<RoutingModule> {

		private final Scenario scenario;
		private final RoutingModule carRouter;

		@Inject
		DriverRoutingModuleProvider(Scenario scenario, @Named(TransportMode.car) RoutingModule carRouter) {
			this.scenario = scenario;
			this.carRouter = carRouter;
		}

		@Override
		public RoutingModule get() {
			return new DriverRoutingModule(
					JointActingTypes.DRIVER,
					scenario.getPopulation().getFactory(),
					carRouter);
		}
	}

	private static class PassengerRoutingModuleProvider implements Provider<RoutingModule> {

		private final Scenario scenario;

		@Inject
		PassengerRoutingModuleProvider(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public RoutingModule get() {
			return new PassengerRoutingModule(
					JointActingTypes.PASSENGER,
					scenario.getPopulation().getFactory());
		}
	}
}

