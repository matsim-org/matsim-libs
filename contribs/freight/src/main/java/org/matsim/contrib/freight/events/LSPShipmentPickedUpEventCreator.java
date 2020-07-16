package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Pickup;

public final class LSPShipmentPickedUpEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof ActivityEndEvent) {
			if(event.getEventType().equals(FreightConstants.PICKUP)) {
				Pickup pickup = (Pickup) activity;
				return new ShipmentPickedUpEvent(carrier.getId(), driverId, pickup.getShipment(), event.getTime());
			}
		}
		return null;
	}
}
