package org.matsim.contrib.shared_mobility.service.events;

import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.core.api.internal.HasPersonId;

public abstract class AbstractSharingEvent extends Event implements HasPersonId, HasLinkId {
	private final Id<SharingService> serviceId;

	private final Id<Link> linkId;

	private final Id<Person> personId;

	protected final Optional<Id<SharingVehicle>> vehicleId;
	protected final Optional<Id<SharingStation>> stationId;
	protected final Optional<Id<Link>> destinationLinkId;

	public AbstractSharingEvent(double time, Id<SharingService> serviceId, Id<Person> personId, Id<Link> linkId,
			Optional<Id<SharingVehicle>> vehicleId, Optional<Id<SharingStation>> stationId, Optional<Id<Link>> destinationLinkId) {
		super(time);

		this.serviceId = serviceId;
		this.linkId = linkId;
		this.personId = personId;

		this.vehicleId = vehicleId;
		this.stationId = stationId;
		this.destinationLinkId = destinationLinkId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("service", serviceId.toString());
		attributes.put("link", linkId.toString());
		attributes.put("person", personId.toString());
		attributes.put("destinationLinkId",destinationLinkId.toString());

		if (vehicleId.isPresent()) {
			attributes.put("vehicle", vehicleId.get().toString());
		}

		if (stationId.isPresent()) {
			attributes.put("station", stationId.get().toString());
		}

		if (destinationLinkId.isPresent()) {
			attributes.put("destinationLinkId", destinationLinkId.get().toString());
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
	
	public Id<SharingVehicle> getSharingVehicleId() {
		return vehicleId.get();
	}
	public Id<Link> getDestinationLinkId() {
		return destinationLinkId.get();
	}
}
