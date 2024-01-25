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

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.vehicles.Vehicle;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargingEventSequenceCollector
		implements QueuedAtChargerEventHandler, QuitQueueAtChargerEventHandler, ChargingStartEventHandler, ChargingEndEventHandler {

	public static class ChargingSequence {
		@Nullable //null if no queueing occurred
		private final QueuedAtChargerEvent queuedAtCharger;
		@Nullable //null if queue was never quit early, i.e. if chargingStartEvent != null;
		private QuitQueueAtChargerEvent quitQueueEvent;
		@Nullable //null if queue was quit early, i.e. if QuitQueueAtChargerEvent != null;
		private ChargingStartEvent chargingStartEvent;
		@Nullable //null if quitQueueEvent != null OR if charging was never completed
		private ChargingEndEvent chargingEndEvent;

		public ChargingSequence(@Nullable QueuedAtChargerEvent queuedAtCharger) {
			this.queuedAtCharger = queuedAtCharger;
		}

		@Nullable
		public Optional<QueuedAtChargerEvent> getQueuedAtCharger() {
			return Optional.ofNullable(queuedAtCharger);
		}

		@Nullable
		public Optional<QuitQueueAtChargerEvent> getQuitQueueAtChargerEvent() {
			return Optional.ofNullable(quitQueueEvent);
		}

		@Nullable
		public Optional<ChargingStartEvent> getChargingStart() {
			return Optional.ofNullable(chargingStartEvent);
		}

		@Nullable
		public Optional<ChargingEndEvent> getChargingEnd() {
			return Optional.ofNullable(chargingEndEvent);
		}
	}

	private final Map<Id<Vehicle>, ChargingSequence> ongoingSequences = new IdMap<>(Vehicle.class);
	private final List<ChargingSequence> completedSequences = new ArrayList<>();

	public List<ChargingSequence> getCompletedSequences() {
		return Collections.unmodifiableList(completedSequences);
	}

	public Set<ChargingSequence> getOnGoingSequences() {
		return ongoingSequences.values().stream().collect(Collectors.toUnmodifiableSet());
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
		sequence.quitQueueEvent = event;
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
