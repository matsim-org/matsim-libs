package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;

public final class LSPFreightLinkEnterEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof LinkEnterEvent) {
			LinkEnterEvent enterEvent = (LinkEnterEvent) event;
			return new LSPFreightLinkEnterEvent(carrier.getId(), scheduledTour.getVehicle().getVehicleId(), driverId, enterEvent.getLinkId(), enterEvent.getTime(), scheduledTour.getVehicle());
		}
		return null;
	}	
}
