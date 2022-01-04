package org.matsim.contrib.freight.events.eventsCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.LSPTourEndEvent;

/*package-private*/ final class LSPTourEndEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof ActivityStartEvent) {
			ActivityStartEvent startEvent = (ActivityStartEvent) event;
			if(startEvent.getActType().equals(FreightConstants.END)) {
				return new LSPTourEndEvent(carrier.getId(),  driverId, scheduledTour.getTour(), startEvent.getTime(), scheduledTour.getVehicle());
			}
		}	
		return null;
	}

	

}
