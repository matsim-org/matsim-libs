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
public class EndRentalEvent extends Event implements HasPersonId{

	private final Id<Link> linkId;
	
	private final Id<Person> personId;
	
	private final String vehicleId;
	
	public static final String EVENT_TYPE = "Rental End";

	public EndRentalEvent(double time, Id<Link> linkId, Id<Person> personId, String vehicleId) {
		super(time);
		this.linkId = linkId;
		this.personId = personId;
		this.vehicleId = vehicleId;		
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id<Link> getLinkId(){
		return this.linkId;
	}
	
	public Id<Person> getPersonId(){
		return this.personId;
	}

	public String getvehicleId() {
		return vehicleId;
	}

}
