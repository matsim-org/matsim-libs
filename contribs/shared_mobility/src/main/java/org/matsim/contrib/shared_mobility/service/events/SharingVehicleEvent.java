package org.matsim.contrib.shared_mobility.service.events;

import java.util.Map;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.service.SharingService;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;

public class SharingVehicleEvent extends Event implements HasLinkId {
	static public final String TYPE = "sharing vehicle placed";

	private final Id<SharingService> serviceId;
	private final Id<Link> linkId;
	private final Id<SharingVehicle> vehicleId;
	private final Optional<Id<SharingStation>> stationId;

	public SharingVehicleEvent(double time, Id<SharingService> serviceId, Id<Link> linkId, Id<SharingVehicle> vehicleId,
			Optional<Id<SharingStation>> stationId) {
		super(time);

		this.serviceId = serviceId;
		this.linkId = linkId;
		this.vehicleId = vehicleId;
		this.stationId = stationId;
	}

	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		attributes.put("service", serviceId.toString());
		attributes.put("link", linkId.toString());
		attributes.put("vehicle", vehicleId.toString());

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
	public String getEventType() {
		return TYPE;
	}

	public Id<SharingService> getServiceId() {
		return serviceId;
	}

	public Id<SharingVehicle> getSharingVehicleId() {
		return vehicleId;
	}

	public Optional<Id<SharingStation>> getStationId() {
		return stationId;
	}

	static public SharingVehicleEvent convert(GenericEvent event) {
		return new SharingVehicleEvent(event.getTime(), //
				Id.create(event.getAttributes().get("service"), SharingService.class), //
				Id.createLinkId(event.getAttributes().get("link")), //
				Id.create(event.getAttributes().get("vehicle"), SharingVehicle.class), //
				Optional.ofNullable(event.getAttributes().get("station")).map(id -> Id.create(id, SharingStation.class)) //
		);
	}
}
