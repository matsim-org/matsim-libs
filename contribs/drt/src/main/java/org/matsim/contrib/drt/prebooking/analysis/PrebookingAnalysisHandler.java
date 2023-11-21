package org.matsim.contrib.drt.prebooking.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.prebooking.PassengerRequestBookedEvent;
import org.matsim.contrib.drt.prebooking.PassengerRequestBookedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEvent;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEventHandler;

public class PrebookingAnalysisHandler implements PassengerRequestBookedEventHandler,
		PassengerRequestSubmittedEventHandler, PassengerRequestScheduledEventHandler,
		PassengerRequestRejectedEventHandler, PassengerWaitingEventHandler {
	private final String mode;
	private final IdMap<Request, Sequence> sequences = new IdMap<>(Request.class);

	public PrebookingAnalysisHandler(String mode) {
		this.mode = mode;
	}

	@Override
	public void handleEvent(PassengerRequestBookedEvent event) {
		sequences.put(event.getRequestId(), new Sequence(event));
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}

		Sequence sequence = sequences.get(event.getRequestId());

		if (sequence != null) {
			sequence.submitted = event;
		}
	}

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}

		Sequence sequence = sequences.get(event.getRequestId());

		if (sequence != null && sequence.rejected == null) { // only use first
			sequence.rejected = event;
		}
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}

		Sequence sequence = sequences.get(event.getRequestId());

		if (sequence != null) {
			sequence.scheduled = event;
		}
	}

	@Override
	public void handleEvent(PassengerWaitingEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}

		Sequence sequence = sequences.get(event.getRequestId());

		if (sequence != null) {
			sequence.waiting = event;
		}
	}

	public List<RequestRecord> getRecords() {
		List<RequestRecord> records = new LinkedList<>();

		for (Sequence sequence : sequences) {
			records.add(new RequestRecord(sequence.booked.getRequestId(), sequence.booked.getPersonIds(),
					sequence.submitted != null ? sequence.submitted.getTime() : null,
					sequence.scheduled != null ? sequence.scheduled.getTime() : null,
					sequence.rejected != null ? sequence.rejected.getTime() : null,
					sequence.waiting != null ? sequence.waiting.getTime() : null,
					sequence.rejected != null ? sequence.rejected.getCause() : null));
		}

		return records;
	}

	public record RequestRecord(Id<Request> requestId, List<Id<Person>> personIds, Double submissionTime, Double scheduledTime,
			Double rejectedTime, Double departureTime, String rejectedReason) {
	}

	private class Sequence {
		final PassengerRequestBookedEvent booked;
		PassengerRequestSubmittedEvent submitted;
		PassengerRequestScheduledEvent scheduled;
		PassengerRequestRejectedEvent rejected;
		PassengerWaitingEvent waiting;

		Sequence(PassengerRequestBookedEvent booked) {
			this.booked = booked;
		}
	}
}
