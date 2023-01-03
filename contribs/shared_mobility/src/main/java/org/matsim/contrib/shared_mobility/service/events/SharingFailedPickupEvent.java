package org.matsim.contrib.shared_mobility.service.events;

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingStation;

public class SharingFailedPickupEvent extends AbstractSharingEvent {
	static public final String TYPE = "sharing vehicle pickup failed";

	public SharingFailedPickupEvent(double time, Id<SharingService> serviceId, Id<Person> personId, Id<Link> linkId,
			Optional<Id<SharingStation>> stationId) {
		super(time, serviceId, personId, linkId, Optional.empty(), stationId, Optional.empty());
	}

	@Override
	public String getEventType() {
		return TYPE;
	}

	static public SharingFailedPickupEvent convert(GenericEvent event) {
		return new SharingFailedPickupEvent(event.getTime(), //
				Id.create(event.getAttributes().get("service"), SharingService.class), //
				Id.createPersonId(event.getAttributes().get("person")), //
				Id.createLinkId(event.getAttributes().get("link")), //
				Optional.ofNullable(event.getAttributes().get("station")).map(id -> Id.create(id, SharingStation.class)) //
		);
	}
}
