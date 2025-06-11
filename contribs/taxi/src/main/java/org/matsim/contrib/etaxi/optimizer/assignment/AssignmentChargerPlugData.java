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

package org.matsim.contrib.etaxi.optimizer.assignment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import org.matsim.contrib.ev.charging.ChargingEstimations;
import org.matsim.contrib.ev.charging.ChargingWithAssignmentLogic;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;

class AssignmentChargerPlugData {
	static class ChargerPlug {
		public final Charger charger;
		public final int idx;

		private ChargerPlug(Charger charger, int idx) {
			this.charger = charger;
			this.idx = idx;
		}
	}

	static AssignmentDestinationData<ChargerPlug> create(double currentTime, Iterable<? extends Charger> chargers) {
		ImmutableList.Builder<DestEntry<ChargerPlug>> builder = ImmutableList.builder();

		int idx = 0;
		for (Charger c : chargers) {
			ChargingWithAssignmentLogic logic = (ChargingWithAssignmentLogic)c.getLogic();

			int dispatched = logic.getAssignedVehicles().size();
			int queued = logic.getQueuedVehicles().size();
			int plugged = logic.getPluggedVehicles().size();

			int assignedVehicles = plugged + queued + dispatched;
			if (assignedVehicles == 2 * c.getPlugCount()) {
				continue;
			} else if (assignedVehicles > 2 * c.getPlugCount()) {
				throw new IllegalStateException();// XXX temp check
			}

			int unassignedPlugs = Math.max(c.getPlugCount() - assignedVehicles, 0);
			for (int p = 0; p < unassignedPlugs; p++) {
				ChargerPlug plug = new ChargerPlug(c, p);
				builder.add(new DestEntry<>(idx++, plug, c.getLink(), currentTime));
			}

			// we do not want to have long queues at chargers: 1 awaiting veh per plug is the limit
			// moreover, in a single run we can assign up to one veh to each plug
			// (sequencing is not possible with AP)
			int assignableVehicles = Math.min(2 * c.getPlugCount() - assignedVehicles, c.getPlugCount());
			if (assignableVehicles == unassignedPlugs) {
				continue;
			}

			// does not include AUX+driving for assigned vehs
			double assignedWorkload = ChargingEstimations.estimateTotalTimeToCharge(
					Streams.concat(logic.getPluggedVehicles().stream(), logic.getQueuedVehicles().stream(),
							logic.getAssignedVehicles().stream()));

			double chargeStart = currentTime + assignedWorkload / (c.getPlugCount() - unassignedPlugs);
			for (int p = unassignedPlugs; p < assignableVehicles; p++) {
				ChargerPlug plug = new ChargerPlug(c, p);
				builder.add(new DestEntry<>(idx++, plug, c.getLink(), chargeStart));
			}
		}

		return new AssignmentDestinationData<>(builder.build());
	}
}
