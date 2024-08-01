package org.matsim.contrib.drt.prebooking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.AbstractPassengerRequestEvent;

import java.util.*;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerRequestBookedEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "PassengerRequest booked";

	public PassengerRequestBookedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds) {
		super(time, mode, requestId, personIds);
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
		String[] personIdsAttribute = attributes.get(ATTRIBUTE_PERSON).split(",");
		List<Id<Person>> personIds = Arrays.stream(personIdsAttribute).map(Id::createPersonId).toList();
		return new PassengerRequestBookedEvent(time, mode, requestId, personIds);
	}
}
