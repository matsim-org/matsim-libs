package org.matsim.contrib.freight.events.eventsCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.events.ShipmentDeliveredEvent;

/*package-private*/  final class LSPShipmentDeliveredEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof ActivityEndEvent) {
			if(event.getEventType().equals(FreightConstants.DELIVERY)) {
				Delivery delivery = (Delivery) activity;
				return new ShipmentDeliveredEvent(carrier.getId(), driverId, delivery.getShipment(), event.getTime());
			}
		}
		return null;
	}
}
