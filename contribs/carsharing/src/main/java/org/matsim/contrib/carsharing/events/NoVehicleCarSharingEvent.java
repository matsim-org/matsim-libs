package org.matsim.contrib.carsharing.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public class NoVehicleCarSharingEvent extends Event{

	public static final String EVENT_TYPE = "no carsharing vehicle";
	
	private final Id<Link> originLinkId;

	private final Id<Link> destinationLinkId;

	private final String carsharingType;

	private String companyId;

	public NoVehicleCarSharingEvent(double time, String carsharingType, String companyId, Link currentLink, Link destinationLink) {
		super(time);
		this.originLinkId = currentLink.getId();
		this.destinationLinkId = destinationLink.getId();
		this.carsharingType = carsharingType;
		this.companyId = companyId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public Id<Link> getOriginLinkId(){
		return this.originLinkId;
	}

	public Id<Link> getDestinationLinkId(){
		return this.destinationLinkId;
	}

	public String getCarsharingType() {
		return this.carsharingType;
	}

	public String getCompanyId() {
		return this.companyId;
	}
}
