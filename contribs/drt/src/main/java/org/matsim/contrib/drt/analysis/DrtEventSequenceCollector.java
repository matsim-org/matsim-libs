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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
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
		PersonMoneyEventHandler, PersonDepartureEventHandler {

	public static class EventSequence {
		private final DrtRequestSubmittedEvent submitted;
		
		@Nullable
		private PassengerRequestScheduledEvent scheduled;
		@Nullable
		private PassengerRequestRejectedEvent rejected;
		
		@Nullable
		private PersonDepartureEvent departure;
		@Nullable
		private PassengerPickedUpEvent pickedUp;
		@Nullable
		private PassengerDroppedOffEvent droppedOff;
		@Nullable
		private List<PersonMoneyEvent> drtFares = new LinkedList<>();

		EventSequence(DrtRequestSubmittedEvent submitted) {
			this.submitted = Objects.requireNonNull(submitted);
		}
		
		public EventSequence(PersonDepartureEvent departed, DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled, PassengerPickedUpEvent pickedUp,
				PassengerDroppedOffEvent droppedOff, List<PersonMoneyEvent> drtFares) {
			this.submitted = Objects.requireNonNull(submitted);
			this.scheduled = scheduled;
			this.departure = departed;
			this.pickedUp = pickedUp;
			this.droppedOff = droppedOff;
			this.drtFares = new ArrayList<>(drtFares);
		}

		public DrtRequestSubmittedEvent getSubmitted() {
			return submitted;
		}

		public Optional<PassengerRequestScheduledEvent> getScheduled() {
			return Optional.ofNullable(scheduled);
		}
		
		public Optional<PassengerRequestRejectedEvent> getRejected() {
			return Optional.ofNullable(rejected);
		}
		
		public Optional<PersonDepartureEvent> getDeparted() {
			return Optional.ofNullable(departure);
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

	private final String mode;
	
	private final Map<Id<Request>, EventSequence> sequences = new HashMap<>();
	private final List<PersonMoneyEvent> drtFarePersonMoneyEvents = new ArrayList<>();
	
	private final Map<Id<Person>, List<EventSequence>> sequencesWithoutDeparture = new HashMap<>();
	private final Map<Id<Person>, List<PersonDepartureEvent>> departuresWithoutSequence = new HashMap<>();

	public DrtEventSequenceCollector(String mode) {
		this.mode = mode;
	}

	public Map<Id<Request>, DrtRequestSubmittedEvent> getRequestSubmissions() {
		return sequences.entrySet().stream() //
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().submitted));
	}

	public Map<Id<Request>, EventSequence> getRejectedRequestSequences() {
		return sequences.entrySet().stream() //
				.filter(e -> e.getValue().getRejected().isPresent()) //
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	public Map<Id<Request>, EventSequence> getPerformedRequestSequences() {
		return sequences.entrySet().stream() //
				.filter(e -> e.getValue().getRejected().isEmpty()) //
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	public List<PersonMoneyEvent> getDrtFarePersonMoneyEvents() {
		return drtFarePersonMoneyEvents;
	}

	@Override
	public void reset(int iteration) {
		sequences.clear();
		drtFarePersonMoneyEvents.clear();
		sequencesWithoutDeparture.clear();
		departuresWithoutSequence.clear();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			EventSequence sequence = new EventSequence(event);
			sequences.put(event.getRequestId(), sequence);
			
			PersonDepartureEvent departureEvent = popDepartureForSubmission(event);
			
			if (departureEvent == null) {
				// We have a submitted request, but no departure event yet (i.e. a prebooking). We start the
				// sequence and note down the person id to fill in the departure event later on.
				sequencesWithoutDeparture.computeIfAbsent(event.getPersonId(), id -> new LinkedList<>()).add(sequence);
			} else {
				sequence.departure = departureEvent;
			}
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			EventSequence sequence = popSequenceForDeparture(event);
			
			if (sequence == null) {
				// We have a departure event, but no submission yet (i.e. and instant booking).
				// We note down the departure event here to recover it later when the submission
				// is down (usually right after).
				departuresWithoutSequence.computeIfAbsent(event.getPersonId(), id -> new LinkedList<>()).add(event);
			} else {
				sequence.departure = event;
			}
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(mode)) {
			sequences.get(event.getRequestId()).scheduled = event;
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (event.getMode().equals(mode)) {
			sequences.get(event.getRequestId()).rejected = event;
		}
	}

	@Override
	public void handleEvent(PassengerPickedUpEvent event) {
		if (event.getMode().equals(mode)) {
			sequences.get(event.getRequestId()).pickedUp = event;
		}
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			sequences.get(event.getRequestId()).droppedOff = event;
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
			EventSequence sequence = sequences.get(
					Id.create(event.getReference(), Request.class));
			if (sequence != null) {
				sequence.drtFares.add(event);
			}
		}
	}

	/*
	 * This function is helper that finds a PersonDepartureEvent for a given
	 * DrtRequestSubmittedEvent. This means that the departure has happened before
	 * submission, which is the usually case for instant requests.
	 */
	private PersonDepartureEvent popDepartureForSubmission(DrtRequestSubmittedEvent event) {
		List<PersonDepartureEvent> potentialDepartures = departuresWithoutSequence.get(event.getPersonId());
		PersonDepartureEvent result = null;

		if (potentialDepartures != null) {
			Iterator<PersonDepartureEvent> iterator = potentialDepartures.iterator();

			while (iterator.hasNext()) {
				PersonDepartureEvent departedEvent = iterator.next();

				if (event.getFromLinkId().equals(departedEvent.getLinkId())) {
					if (result != null) {
						throw new IllegalStateException(
								"Ambiguous matching between submission and departure - not sure how to solve this");
					}

					iterator.remove();
					result = departedEvent;
				}
			}

			if (potentialDepartures.size() == 0) {
				departuresWithoutSequence.remove(event.getPersonId());
			}
		}

		return result;
	}
	
	/*
	 * This function is helper that finds a sequence given a PersonDepartureEvent.
	 * This means that a sequence has started (the request has been submitted)
	 * before the agent has departed, i.e. this is a pre-booking of some sort.
	 */
	private EventSequence popSequenceForDeparture(PersonDepartureEvent event) {
		EventSequence result = null;
		List<EventSequence> potentialSequences = sequencesWithoutDeparture.get(event.getPersonId());

		if (potentialSequences != null) {
			Iterator<EventSequence> iterator = potentialSequences.iterator();

			while (iterator.hasNext()) {
				EventSequence sequence = iterator.next();

				if (sequence.submitted.getFromLinkId().equals(event.getLinkId())) {
					if (result != null) {
						throw new IllegalStateException(
								"Ambiguous matching between submission and departure - not sure how to solve this");
					}

					iterator.remove();
					result = sequence;
				}
			}

			if (potentialSequences.size() == 0) {
				sequencesWithoutDeparture.remove(event.getPersonId());
			}
		}
		
		return result;
	}
}
