/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ChargingWithQueueingAndAssignmentLogic extends ChargingWithQueueingLogic
		implements ChargingWithAssignmentLogic {
	private final Map<Id<Vehicle>, ElectricVehicle> assignedVehicles = new LinkedHashMap<>();

	public ChargingWithQueueingAndAssignmentLogic(ChargerSpecification charger, ChargingStrategy chargingStrategy,
			EventsManager eventsManager) {
		super(charger, chargingStrategy, eventsManager);
	}

	@Override
	public void assignVehicle(ElectricVehicle ev) {
		if (assignedVehicles.put(ev.getId(), ev) != null) {
			throw new IllegalArgumentException("Vehicle is already assigned: " + ev.getId());
		}
	}

	@Override
	public void unassignVehicle(ElectricVehicle ev) {
		if (assignedVehicles.remove(ev.getId()) == null) {
			throw new IllegalArgumentException("Vehicle was not assigned: " + ev.getId());
		}
	}

	private final Collection<ElectricVehicle> unmodifiableAssignedVehicles = Collections.unmodifiableCollection(
			assignedVehicles.values());

	@Override
	public Collection<ElectricVehicle> getAssignedVehicles() {
		return unmodifiableAssignedVehicles;
	}

	public static class FactoryProvider implements Provider<ChargingLogic.Factory> {
		@Inject
		private EventsManager eventsManager;

		private final Function<ChargerSpecification, ChargingStrategy> chargingStrategyCreator;

		public FactoryProvider(Function<ChargerSpecification, ChargingStrategy> chargingStrategyCreator) {
			this.chargingStrategyCreator = chargingStrategyCreator;
		}

		@Override
		public ChargingLogic.Factory get() {
			return charger -> new ChargingWithQueueingAndAssignmentLogic(charger,
					chargingStrategyCreator.apply(charger), eventsManager);
		}
	}
}
