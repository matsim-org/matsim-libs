package org.matsim.contrib.freight.events.eventsCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.LSPFreightLinkLeaveEvent;

/*package-private*/  final class LSPFreightLinkLeaveEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId,int activityCounter) {
		if((event instanceof LinkLeaveEvent)) {
			LinkLeaveEvent  leaveEvent = (LinkLeaveEvent) event;
			return new LSPFreightLinkLeaveEvent(carrier.getId(), scheduledTour.getVehicle().getVehicleId(), driverId, leaveEvent.getLinkId(), leaveEvent.getTime(), scheduledTour.getVehicle());
		}	
		return null;
	}
}
