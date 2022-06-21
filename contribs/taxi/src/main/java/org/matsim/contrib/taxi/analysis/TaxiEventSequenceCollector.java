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

package org.matsim.contrib.taxi.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;

/**
 * Creates RequestEventSequence that contains almost all data for request/leg analysis.
 *
 * @author Michal Maciejewski (michalm)
 */
public class TaxiEventSequenceCollector
		implements PassengerRequestScheduledEventHandler, PassengerRequestSubmittedEventHandler,
		PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {

	public static class RequestEventSequence {
		private final PassengerRequestSubmittedEvent submitted;
		//nullable events may be null if QSim ends before the request is actually handled
		@Nullable
		private PassengerRequestScheduledEvent scheduled;
		@Nullable
		private PassengerPickedUpEvent pickedUp;
		@Nullable
		private PassengerDroppedOffEvent droppedOff;

		public RequestEventSequence(PassengerRequestSubmittedEvent submitted) {
			this(submitted, null, null, null);
		}

		public RequestEventSequence(PassengerRequestSubmittedEvent submitted, PassengerRequestScheduledEvent scheduled,
				PassengerPickedUpEvent pickedUp, PassengerDroppedOffEvent droppedOff) {
			this.submitted = Objects.requireNonNull(submitted);
			this.scheduled = scheduled;
			this.pickedUp = pickedUp;
			this.droppedOff = droppedOff;
		}

		public PassengerRequestSubmittedEvent getSubmitted() {
			return submitted;
		}

		public Optional<PassengerRequestScheduledEvent> getScheduled() {
			return Optional.ofNullable(scheduled);
		}

		public Optional<PassengerPickedUpEvent> getPickedUp() {
			return Optional.ofNullable(pickedUp);
		}

		public Optional<PassengerDroppedOffEvent> getDroppedOff() {
			return Optional.ofNullable(droppedOff);
		}

		public boolean isCompleted() {
			return droppedOff != null;
		}
	}

	private final String mode;
	private final Map<Id<Request>, RequestEventSequence> requestSequences = new HashMap<>();

	public TaxiEventSequenceCollector(String mode) {
		this.mode = mode;
	}

	public Map<Id<Request>, RequestEventSequence> getRequestSequences() {
		return Collections.unmodifiableMap(requestSequences);
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			requestSequences.put(event.getRequestId(), new RequestEventSequence(event));
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			requestSequences.get(event.getRequestId()).scheduled = event;
		}
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		if (event.getMode().equals(mode)) {
			requestSequences.get(event.getRequestId()).pickedUp = event;
		}
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			requestSequences.get(event.getRequestId()).droppedOff = event;
		}
	}

	@Override
	public void reset(int iteration) {
		requestSequences.clear();
	}
}
