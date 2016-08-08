package org.matsim.contrib.carsharing.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

public class StartRentalEvent extends Event implements HasPersonId{

	private final Id<Link> originlinkId;
	
	private final Id<Link> pickuplinkId;
	
	private final Id<Person> personId;
	
	private final String vehicleId;
	
	public static final String EVENT_TYPE = "Rental Start";

	public StartRentalEvent(double time, Id<Link> originlinkId, 
			Id<Link> pickuplinkId, Id<Person> personId, String vehicleId) {
		super(time);
		this.originlinkId = originlinkId;
		this.pickuplinkId = pickuplinkId;

		this.personId = personId;
		this.vehicleId = vehicleId;
		
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
	
	public Id<Person> getPersonId(){
		return this.personId;
	}
	
	public String getvehicleId(){
		return this.vehicleId;
	}
	
	

}
