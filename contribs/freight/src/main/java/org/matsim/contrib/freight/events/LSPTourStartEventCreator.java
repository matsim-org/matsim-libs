package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;

public final class LSPTourStartEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId, int activityCounter) {
		if((event instanceof ActivityEndEvent)) {
			ActivityEndEvent endEvent = (ActivityEndEvent) event;
			if(endEvent.getActType().equals(FreightConstants.START)) {
				return new LSPTourStartEvent(carrier.getId(), driverId, scheduledTour.getTour(), event.getTime(), scheduledTour.getVehicle());
			}	
		}
		return null;	
	}

}
