package org.matsim.contrib.freight.events.eventsCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.LSPFreightLinkEnterEvent;

/*package-private*/ final class LSPFreightLinkEnterEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof LinkEnterEvent) {
			LinkEnterEvent enterEvent = (LinkEnterEvent) event;
			return new LSPFreightLinkEnterEvent(carrier.getId(), scheduledTour.getVehicle().getId(), driverId, enterEvent.getLinkId(), enterEvent.getTime(), scheduledTour.getVehicle());
		}
		return null;
	}	
}
