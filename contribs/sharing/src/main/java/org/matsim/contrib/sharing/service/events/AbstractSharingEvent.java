package org.matsim.contrib.sharing.service.events;

import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.sharing.service.SharingService;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;
import org.matsim.core.api.internal.HasPersonId;

public abstract class AbstractSharingEvent extends Event implements HasPersonId, HasLinkId {
	private final Id<SharingService> serviceId;

	private final Id<Link> linkId;
	private final Id<Person> personId;

	protected final Optional<Id<SharingVehicle>> vehicleId;
	protected final Optional<Id<SharingStation>> stationId;

	public AbstractSharingEvent(double time, Id<SharingService> serviceId, Id<Person> personId, Id<Link> linkId,
			Optional<Id<SharingVehicle>> vehicleId, Optional<Id<SharingStation>> stationId) {
		super(time);

		this.serviceId = serviceId;
		this.linkId = linkId;
		this.personId = personId;

		this.vehicleId = vehicleId;
		this.stationId = stationId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("service", serviceId.toString());
		attributes.put("link", linkId.toString());
		attributes.put("person", personId.toString());

		if (vehicleId.isPresent()) {
			attributes.put("vehicle", vehicleId.get().toString());
		}

		if (stationId.isPresent()) {
			attributes.put("station", stationId.get().toString());
		}

		return attributes;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}
	
	public Id<SharingService> getServiceId() {
		return serviceId;
	}
}
