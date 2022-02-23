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

package org.matsim.contrib.ev.charging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingEventSequenceCollector
		implements QueuedAtChargerEventHandler, QuitQueueAtChargerEventHandler, ChargingStartEventHandler, ChargingEndEventHandler {

	public static class ChargingSequence {
		@Nullable //null if no queueing occurred
		private final QueuedAtChargerEvent queuedAtCharger;
		private ChargingStartEvent chargingStartEvent;
		private ChargingEndEvent chargingEndEvent;

		public ChargingSequence(@Nullable QueuedAtChargerEvent queuedAtCharger) {
			this.queuedAtCharger = queuedAtCharger;
		}

		@Nullable
		public Optional<QueuedAtChargerEvent> getQueuedAtCharger() {
			return Optional.ofNullable(queuedAtCharger);
		}

		public ChargingStartEvent getChargingStart() {
			return chargingStartEvent;
		}

		public ChargingEndEvent getChargingEnd() {
			return chargingEndEvent;
		}
	}

	private final Map<Id<ElectricVehicle>, ChargingSequence> ongoingSequences = new IdMap<>(ElectricVehicle.class);
	private final List<ChargingSequence> completedSequences = new ArrayList<>();

	public List<ChargingSequence> getCompletedSequences() {
		return Collections.unmodifiableList(completedSequences);
	}

	@Override
	public void handleEvent(QueuedAtChargerEvent event) {
		Preconditions.checkState(ongoingSequences.put(event.getVehicleId(), new ChargingSequence(event)) == null,
				"Vehicle (%s) already at a charger", event.getVehicleId());
	}

	@Override
	public void handleEvent(QuitQueueAtChargerEvent event) {
		var sequence = ongoingSequences.remove(event.getVehicleId());
		Preconditions.checkState(sequence != null, "Vehicle (%s) was not at charger (%s)", event.getVehicleId(),
				event.getChargerId());
		Preconditions.checkState(sequence.queuedAtCharger != null, "Vehicle (%s) was not queued at charger (%s)",
				event.getVehicleId(), event.getChargerId());
		Preconditions.checkState(sequence.chargingStartEvent == null, "Vehicle (%s) is already plugged",
				event.getVehicleId(), event.getChargerId());
		completedSequences.add(sequence);
	}

	@Override
	public void handleEvent(ChargingStartEvent event) {
		//create charging sequence (if queueing has not occurred)
		var sequence = ongoingSequences.computeIfAbsent(event.getVehicleId(),
				electricVehicleId -> new ChargingSequence(null));
		sequence.chargingStartEvent = event;
	}

	@Override
	public void handleEvent(ChargingEndEvent event) {
		var sequence = ongoingSequences.remove(event.getVehicleId());
		sequence.chargingEndEvent = event;
		completedSequences.add(sequence);
	}

	@Override
	public void reset(int iteration) {
		ongoingSequences.clear();
		completedSequences.clear();
	}
}
