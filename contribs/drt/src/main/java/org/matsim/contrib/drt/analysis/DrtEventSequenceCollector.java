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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEvent;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEventHandler;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * Creates PerformedRequestEventSequence (for scheduled requests) and
 * RejectedRequestEventSequence (for rejected requests). Almost all data for
 * request/leg analysis is there (except info on actual paths), so should be
 * quite reusable.
 *
 * Without prebooking, the order of sequences is always the same: First the
 * agent *departs* then the request is *submitted*, then it is *rejected* or
 * picked up. With prebooking, the order of departure and submission can be
 * reversed: An agent first submits the request, and only later departs on the
 * leg. Since *departure* is core MATSim, the respective event has no
 * information about the request identifier. It could now happen that two
 * submission with same characteristics have been submitted (person id, origin
 * id). Then it is not clear which request belongs to the current departure. For
 * that purpose the PassengerWaiting event has been introduced which is fired
 * right after a departure has been processed by a DRT-related PassengerEngine.
 * This events allows to link the latest departure of an agent to a request id.
 *
 * @author jbischoff
 * @author Michal Maciejewski
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DrtEventSequenceCollector
		implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PassengerWaitingEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler,
		PersonMoneyEventHandler, PersonDepartureEventHandler {

	public static class EventSequence {

		public static class PersonEvents {

			@Nullable
			private PersonDepartureEvent departure;
			@Nullable
			private PassengerPickedUpEvent pickedUp;
			@Nullable
			private PassengerDroppedOffEvent droppedOff;

			public Optional<PersonDepartureEvent> getDeparture() {
				return Optional.ofNullable(departure);
			}

			public Optional<PassengerPickedUpEvent> getPickedUp() {
				return Optional.ofNullable(pickedUp);
			}

			public Optional<PassengerDroppedOffEvent> getDroppedOff() {
				return Optional.ofNullable(droppedOff);
			}
		}
		private final DrtRequestSubmittedEvent submitted;

		@Nullable
		private PassengerRequestScheduledEvent scheduled;
		@Nullable
		private PassengerRequestRejectedEvent rejected;

		private final Map<Id<Person>, PersonEvents> personEvents = new HashMap<>();

		@Nullable
		private List<PersonMoneyEvent> drtFares = new LinkedList<>();

		EventSequence(DrtRequestSubmittedEvent submitted) {
			this.submitted = Objects.requireNonNull(submitted);
		}

		public EventSequence(Id<Person> personId, PersonDepartureEvent departure, DrtRequestSubmittedEvent submitted,
				PassengerRequestScheduledEvent scheduled, PassengerPickedUpEvent pickedUp,
				PassengerDroppedOffEvent droppedOff, List<PersonMoneyEvent> drtFares) {
			this.submitted = Objects.requireNonNull(submitted);
			this.scheduled = scheduled;
			this.drtFares = new ArrayList<>(drtFares);
			PersonEvents personEvents = new PersonEvents();
			personEvents.departure = departure;
			personEvents.pickedUp = pickedUp;
			personEvents.droppedOff = droppedOff;
			this.personEvents.put(personId, personEvents);
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

		public Map<Id<Person>, PersonEvents> getPersonEvents() {
			return personEvents;
		}


		public List<PersonMoneyEvent> getDrtFares() {
			return Collections.unmodifiableList(drtFares);
		}

		public boolean isCompleted() {
			return submitted.getPersonIds().stream().allMatch(personId -> { 
				var events = personEvents.get(personId);
				return events != null && personEvents.get(personId).droppedOff != null; 
			});
		}
	}

	private final String mode;

	private final Map<Id<Request>, EventSequence> sequences = new HashMap<>();
	private final List<PersonMoneyEvent> drtFarePersonMoneyEvents = new ArrayList<>();

	private final Map<Id<Person>, PersonDepartureEvent> latestDepartures = new HashMap<>();
	private final Map<Id<Request>, List<PersonDepartureEvent>> waitingForSubmission = new HashMap<>();

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
				.filter(e -> e.getValue().personEvents.values().stream().allMatch(pe -> pe.departure != null)) //
				.filter(e -> e.getValue().personEvents.values().stream().allMatch(pe -> pe.pickedUp != null)) //
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}

	public List<PersonMoneyEvent> getDrtFarePersonMoneyEvents() {
		return drtFarePersonMoneyEvents;
	}

	@Override
	public void reset(int iteration) {
		sequences.clear();
		drtFarePersonMoneyEvents.clear();
		latestDepartures.clear();
		waitingForSubmission.clear();
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (event.getMode().equals(mode)) {
			EventSequence sequence = new EventSequence(event);
			sequences.put(event.getRequestId(), sequence);

			// if we already have a departure
			List<PersonDepartureEvent> waiting = waitingForSubmission.remove(event.getRequestId());
			if(waiting != null) {
				for (PersonDepartureEvent departure : waiting) {
					sequence.getPersonEvents().computeIfAbsent(
							departure.getPersonId(), personId -> new EventSequence.PersonEvents()
					).departure = departure;
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			// note down the departure event here, for now we don't know which request it
			// belongs to, see below
			Preconditions.checkState(!latestDepartures.containsKey(event.getPersonId()),
					"Attempt to register a departure event for " + mode + " and person " + event.getPersonId()
							+ ", but there is still a departure that has not been consumed");
			latestDepartures.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(PassengerWaitingEvent event) {
		if (event.getMode().equals(mode)) {
			EventSequence sequence = sequences.get(event.getRequestId());
			for (Id<Person> personId : event.getPersonIds()) {
				// must exist, otherwise something is wrong
				PersonDepartureEvent departureEvent = Objects.requireNonNull(latestDepartures.remove(personId));

				if (sequence != null) {
					// prebooked request, we already have the submission
					Verify.verifyNotNull(sequence.submitted);
					sequence.personEvents.computeIfAbsent(personId, p -> new EventSequence.PersonEvents()).departure = departureEvent;
				} else {
					// immediate request, submission event should come soon
					waitingForSubmission.computeIfAbsent(event.getRequestId(), requestId -> new ArrayList<>()).add(departureEvent);
				}
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
			sequences.get(event.getRequestId()).personEvents.get(event.getPersonId()).pickedUp = event;
		}
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			sequences.get(event.getRequestId()).personEvents.get(event.getPersonId()).droppedOff = event;
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
}
