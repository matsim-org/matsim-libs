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

package org.matsim.contrib.drt.analysis;

import static org.matsim.contrib.drt.fare.DrtFareHandler.PERSON_MONEY_EVENT_REFERENCE_DRT_FARE_DAILY_FEE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.contrib.drt.fare.DrtFareHandler;
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

import com.google.common.base.Preconditions;

/**
 * Creates PerformedRequestEventSequence (for scheduled requests) and RejectedRequestEventSequence (for rejected requests).
 * Almost all data for request/leg analysis is there (except info on actual paths), so should be quite reusable.
 *
 * @author jbischoff
 * @author Michal Maciejewski
 */
public class DrtEventSequenceCollector
		implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler,
		PersonMoneyEventHandler {

	private static final Logger log = Logger.getLogger(DrtEventSequenceCollector.class);

	public static class PerformedRequestEventSequence {
		private final DrtRequestSubmittedEvent submitted;
		private final PassengerRequestScheduledEvent scheduled;
		//pickedUp and droppedOff may be null if QSim ends before the request is actually handled
		@Nullable
		private PassengerPickedUpEvent pickedUp;
		@Nullable
		private PassengerDroppedOffEvent droppedOff;
		@Nullable
		private List<PersonMoneyEvent> drtFares;

		public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled) {
			this(submitted, scheduled, null, null, List.of());
		}

		public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled, PassengerPickedUpEvent pickedUp,
				PassengerDroppedOffEvent droppedOff, List<PersonMoneyEvent> drtFares) {
			this.submitted = Objects.requireNonNull(submitted);
			this.scheduled = Objects.requireNonNull(scheduled);
			this.pickedUp = pickedUp;
			this.droppedOff = droppedOff;
			this.drtFares = new ArrayList<>(drtFares);
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

		public List<PersonMoneyEvent> getDrtFares() {
			return Collections.unmodifiableList(drtFares);
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
	private final List<PersonMoneyEvent> drtFarePersonMoneyEvents = new ArrayList<>();

	public DrtEventSequenceCollector(String mode) {
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

	public List<PersonMoneyEvent> getDrtFarePersonMoneyEvents() {
		return drtFarePersonMoneyEvents;
	}

	@Override
	public void reset(int iteration) {
		requestSubmissions.clear();
		rejectedRequestSequences.clear();
		performedRequestSequences.clear();
		drtFarePersonMoneyEvents.clear();
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

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (mode.equals(event.getTransactionPartner()) && DrtFareHandler.PERSON_MONEY_EVENT_PURPOSE_DRT_FARE.equals(
				event.getPurpose())) {
			Preconditions.checkNotNull(event.getReference(),
					"Found a PersonMoneyEvent with purpose (%s) and transactionPartner (%s)"
							+ " but without field reference (null)."
							+ " This field should be the drt request id or "
							+ PERSON_MONEY_EVENT_REFERENCE_DRT_FARE_DAILY_FEE
							+ " or similar. Terminating.", event.getPurpose(), event.getTransactionPartner());

			drtFarePersonMoneyEvents.add(event);
			PerformedRequestEventSequence sequence = performedRequestSequences.get(
					Id.create(event.getReference(), Request.class));
			if (sequence != null) {
				sequence.drtFares.add(event);
			}
		}
	}
}
