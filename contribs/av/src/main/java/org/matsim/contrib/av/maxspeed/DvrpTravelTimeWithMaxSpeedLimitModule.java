/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.av.maxspeed;

import javax.inject.Inject;
import javax.inject.Named;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeWithMaxSpeedLimitModule extends AbstractModule {
	private final VehicleType vehicleType;

	public DvrpTravelTimeWithMaxSpeedLimitModule(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	@Override
	public void install() {
		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSource.DVRP_VEHICLE_TYPE)).toInstance(vehicleType);
		bind(QSimFreeSpeedTravelTimeWithMaxSpeedLimit.class).asEagerSingleton();
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).to(QSimFreeSpeedTravelTimeWithMaxSpeedLimit.class);
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_OBSERVED).to(MaxTravelTime.class);
	}

	private static class MaxTravelTime implements TravelTime {
		private final QSimFreeSpeedTravelTimeWithMaxSpeedLimit qSimFreeSpeedTravelTimeWithMaxSpeedLimit;
		private final TravelTime observedCarTravelTime;

		@Inject
		public MaxTravelTime(QSimFreeSpeedTravelTimeWithMaxSpeedLimit qSimFreeSpeedTravelTimeWithMaxSpeedLimit,
				@Named(TransportMode.car) TravelTime observedCarTravelTime) {
			this.qSimFreeSpeedTravelTimeWithMaxSpeedLimit = qSimFreeSpeedTravelTimeWithMaxSpeedLimit;
			this.observedCarTravelTime = observedCarTravelTime;
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return Math.max(qSimFreeSpeedTravelTimeWithMaxSpeedLimit.getLinkTravelTime(link, time, person, vehicle),
					observedCarTravelTime.getLinkTravelTime(link, time, person, vehicle));
		}
	}
}

