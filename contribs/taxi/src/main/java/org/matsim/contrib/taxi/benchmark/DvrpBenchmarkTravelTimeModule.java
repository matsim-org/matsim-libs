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

package org.matsim.contrib.taxi.benchmark;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.trafficmonitoring.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author michalm
 */
public class DvrpBenchmarkTravelTimeModule extends AbstractModule {
	private final TravelTime travelTime;

	public DvrpBenchmarkTravelTimeModule() {
		this(new FreeSpeedTravelTime());
	}

	public DvrpBenchmarkTravelTimeModule(final TravelTime travelTime) {
		this.travelTime = travelTime;
	}

	public void install() {
		// Because TravelTimeCalculatorModule is not installed for benchmarking, we need to add a binding
		// for the car mode
		addTravelTimeBinding(TransportMode.car).toInstance(travelTime);
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_ESTIMATED).toInstance(travelTime);

		// since we cannot undo: addMobsimListenerBinding().to(VrpTravelTimeEstimator.class)
		// that is bound in DvrpModule, we need to bind VrpTravelTimeEstimator.class to this NOP instance
		bind(DvrpTravelTimeEstimator.class).toInstance(new DvrpTravelTimeEstimator() {
			@SuppressWarnings("rawtypes")
			@Override
			public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
			}

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				throw new UnsupportedOperationException();// or travelTime.getLinkTravelTime(....)
			}
		});
	}
}
