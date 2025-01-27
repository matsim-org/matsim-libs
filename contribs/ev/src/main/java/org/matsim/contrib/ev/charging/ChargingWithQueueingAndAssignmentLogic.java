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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

public class ChargingWithQueueingAndAssignmentLogic extends ChargingWithQueueingLogic
		implements ChargingWithAssignmentLogic {
	private final Map<Id<Vehicle>, ChargingVehicle> assignedVehicles = new LinkedHashMap<>();

	public ChargingWithQueueingAndAssignmentLogic(ChargerSpecification charger, EventsManager eventsManager, ChargingPriority priority) {
		super(charger, eventsManager, priority);
	}

	@Override
	public void assignVehicle(ElectricVehicle ev, ChargingStrategy strategy) {
		ChargingVehicle cv = new ChargingVehicle(ev, strategy);
		if (assignedVehicles.put(ev.getId(), cv) != null) {
			throw new IllegalArgumentException("Vehicle is already assigned: " + ev.getId());
		}
	}

	@Override
	public void unassignVehicle(ElectricVehicle ev) {
		if (assignedVehicles.remove(ev.getId()) == null) {
			throw new IllegalArgumentException("Vehicle was not assigned: " + ev.getId());
		}
	}

	@Override
	public boolean isAssigned(ElectricVehicle ev) {
		return assignedVehicles.containsKey(ev.getId());
	}

	private final Collection<ChargingVehicle> unmodifiableAssignedVehicles = Collections.unmodifiableCollection(
			assignedVehicles.values());

	@Override
	public Collection<ChargingVehicle> getAssignedVehicles() {
		return unmodifiableAssignedVehicles;
	}

	static public class Factory implements ChargingLogic.Factory {
		private final EventsManager eventsManager;
		private final ChargingPriority.Factory priorityFactory;

		public Factory(EventsManager eventsManager, ChargingPriority.Factory priorityFactory) {
			this.eventsManager = eventsManager;
			this.priorityFactory = priorityFactory;
		}

		@Override
		public ChargingLogic create(ChargerSpecification charger) {
			return new ChargingWithQueueingAndAssignmentLogic(charger, eventsManager, priorityFactory.create(charger));
		}
	}
}
