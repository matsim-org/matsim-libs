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

package org.matsim.vsp.ev.charging;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

public class ChargingWithQueueingAndAssignmentLogic extends ChargingWithQueueingLogic {
	private final Map<Id<ElectricVehicle>, ElectricVehicle> assignedVehicles = new LinkedHashMap<>();

	public ChargingWithQueueingAndAssignmentLogic(Charger charger, ChargingStrategy chargingStrategy) {
		super(charger, chargingStrategy);
	}

	public void assignVehicle(ElectricVehicle ev) {
		if (assignedVehicles.put(ev.getId(), ev) != null) {
			throw new IllegalArgumentException();
		}
	}

	public void unassignVehicle(ElectricVehicle ev) {
		if (assignedVehicles.remove(ev.getId()) == null) {
			throw new IllegalArgumentException();
		}
	}

	private final Collection<ElectricVehicle> unmodifiableAssignedVehicles = Collections
			.unmodifiableCollection(assignedVehicles.values());

	public Collection<ElectricVehicle> getAssignedVehicles() {
		return unmodifiableAssignedVehicles;
	}
}
