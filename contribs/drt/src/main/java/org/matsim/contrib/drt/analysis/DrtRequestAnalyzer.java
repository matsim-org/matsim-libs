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

/**
 *
 */
package org.matsim.contrib.drt.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

/**
 * Creates PerformedRequestEventSequence (for scheduled requests) and RejectedRequestEventSequence (for rejected requests).
 * Almost all data for request/trip analysis is there (except info on actual paths), so should be quite reusable.
 *
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class DrtRequestAnalyzer implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler {

	public static class PerformedRequestEventSequence {
		private final DrtRequestSubmittedEvent submitted;
		private final PassengerRequestScheduledEvent scheduled;
		//pickedUp and droppedOff may be null if QSim ends before the request is actually handled
		@Nullable
		private PassengerPickedUpEvent pickedUp;
		@Nullable
		private PassengerDroppedOffEvent droppedOff;

		public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled) {
			this(submitted, scheduled, null, null);
		}

		public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled, PassengerPickedUpEvent pickedUp,
				PassengerDroppedOffEvent droppedOff) {
			this.submitted = Objects.requireNonNull(submitted);
			this.scheduled = Objects.requireNonNull(scheduled);
			this.pickedUp = pickedUp;
			this.droppedOff = droppedOff;
		}

		public DrtRequestSubmittedEvent getSubmitted() {
			return submitted;
		}

		public PassengerRequestScheduledEvent getScheduled() {
			return scheduled;
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

	public static class RejectedRequestEventSequence {
		private final DrtRequestSubmittedEvent submitted;
		private final PassengerRequestRejectedEvent rejected;

		public RejectedRequestEventSequence(DrtRequestSubmittedEvent submitted,
				PassengerRequestRejectedEvent rejected) {
			this.submitted = submitted;
			this.rejected = rejected;
		}

		public DrtRequestSubmittedEvent getSubmitted() {
			return submitted;
		}

		public PassengerRequestRejectedEvent getRejected() {
			return rejected;
		}
	}

	private final String mode;
	private final Map<Id<Request>, DrtRequestSubmittedEvent> requestSubmissions = new HashMap<>();
	private final Map<Id<Request>, RejectedRequestEventSequence> rejectedRequestSequences = new HashMap<>();
	private final Map<Id<Request>, PerformedRequestEventSequence> performedRequestSequences = new HashMap<>();

	public DrtRequestAnalyzer(String mode) {
		this.mode = mode;
	}

	public Map<Id<Request>, DrtRequestSubmittedEvent> getRequestSubmissions() {
		return requestSubmissions;
	}

	public Map<Id<Request>, RejectedRequestEventSequence> getRejectedRequestSequences() {
		return rejectedRequestSequences;
	}

	public Map<Id<Request>, PerformedRequestEventSequence> getPerformedRequestSequences() {
		return performedRequestSequences;
	}

	@Override
	public void reset(int iteration) {
		requestSubmissions.clear();
		rejectedRequestSequences.clear();
		performedRequestSequences.clear();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			requestSubmissions.put(event.getRequestId(), event);
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			performedRequestSequences.put(event.getRequestId(),
					new PerformedRequestEventSequence(requestSubmissions.get(event.getRequestId()), event));
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			rejectedRequestSequences.put(event.getRequestId(),
					new RejectedRequestEventSequence(requestSubmissions.get(event.getRequestId()), event));
		}
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		if (event.getMode().equals(mode)) {
			performedRequestSequences.get(event.getRequestId()).pickedUp = event;
		}
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			performedRequestSequences.get(event.getRequestId()).droppedOff = event;
		}
	}
}
