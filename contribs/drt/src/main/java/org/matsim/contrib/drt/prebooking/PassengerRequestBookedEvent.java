package org.matsim.contrib.drt.prebooking;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.AbstractPassengerRequestEvent;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerRequestBookedEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "PassengerRequest booked";

	public PassengerRequestBookedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId) {
		super(time, mode, requestId, List.of(personId));
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static PassengerRequestBookedEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		Id<Person> personId = Id.createPersonId(attributes.get(ATTRIBUTE_PERSON));
		return new PassengerRequestBookedEvent(time, mode, requestId, personId);
	}
}
