package org.matsim.contrib.drt.extension.prebooking.analysis;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerEnteringVehicleEvent;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerEnteringVehicleEventHandler;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerRequestBookedEvent;
import org.matsim.contrib.drt.extension.prebooking.events.PassengerRequestBookedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;

public class PrebookingAnalysisHandler implements PassengerRequestBookedEventHandler,
		PassengerRequestSubmittedEventHandler, PassengerRequestScheduledEventHandler,
		PassengerRequestRejectedEventHandler, PassengerEnteringVehicleEventHandler {
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

		if (sequence != null) {
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
	public void handleEvent(PassengerEnteringVehicleEvent event) {
		if (!event.getMode().equals(mode)) {
			return;
		}

		Sequence sequence = sequences.get(event.getRequestId());

		if (sequence != null) {
			sequence.entering = event;
		}
	}

	public List<RequestRecord> getRecords() {
		List<RequestRecord> records = new LinkedList<>();

		for (Sequence sequence : sequences) {
			records.add(new RequestRecord(sequence.booked.getRequestId(), sequence.booked.getPersonId(),
					sequence.submitted != null ? sequence.submitted.getTime() : null,
					sequence.scheduled != null ? sequence.scheduled.getTime() : null,
					sequence.rejected != null ? sequence.rejected.getTime() : null,
					sequence.entering != null ? sequence.entering.getTime() : null));
		}

		return records;
	}

	public record RequestRecord(Id<Request> requestId, Id<Person> personId, Double submissionTime, Double scheduledTime,
			Double rejectedTime, Double enteringTime) {
	}

	private class Sequence {
		final PassengerRequestBookedEvent booked;
		PassengerRequestSubmittedEvent submitted;
		PassengerEnteringVehicleEvent entering;
		PassengerRequestScheduledEvent scheduled;
		PassengerRequestRejectedEvent rejected;

		Sequence(PassengerRequestBookedEvent booked) {
			this.booked = booked;
		}
	}
}
