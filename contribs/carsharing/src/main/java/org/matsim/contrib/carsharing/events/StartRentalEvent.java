package org.matsim.contrib.carsharing.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
/** 
 * 
 * @author balac
 */
public class StartRentalEvent extends Event implements HasPersonId{

	private final Id<Link> originlinkId;
	
	private final Id<Link> pickuplinkId;

	private final Id<Link> destinationLinkId;

	private final Id<Person> personId;
	
	private final String vehicleId;
	
	private String carsharingType;

	private String companyId;

	public static final String EVENT_TYPE = "Rental Start";

	public StartRentalEvent(double now, String carsharingType, String companyId,
			Link currentLink, Link stationLink, Link destinationLink, Id<Person> id, String vehId) {
		super(now);
		this.originlinkId = currentLink.getId();
		this.pickuplinkId = stationLink.getId();
		this.destinationLinkId = destinationLink.getId();

		this.personId = id;
		this.vehicleId = vehId;
		this.carsharingType = carsharingType;
		this.companyId = companyId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id<Link> getOriginLinkId(){
		return this.originlinkId;
	}
	
	public Id<Link> getPickuplinkId(){
		return this.pickuplinkId;
	}

	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId;
	}

	public Id<Person> getPersonId(){
		return this.personId;
	}
	
	public String getvehicleId(){
		return this.vehicleId;
	}

	public String getCarsharingType() {
		return this.carsharingType;
	}

	public String getCompanyId() {
		return this.companyId;
	}
}
