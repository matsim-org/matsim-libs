package org.matsim.contrib.carsharing.events;

import java.util.LinkedHashMap;
import java.util.Map;

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
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = new LinkedHashMap<String, String>();
		attr.put(ATTRIBUTE_TIME, Double.toString(super.getTime()));
		attr.put(ATTRIBUTE_TYPE, getEventType());
		attr.put("Vehicleid", vehicleId);
		attr.put("personid", personId.toString());
		attr.put("linkid", linkId.toString());

		return attr;
	}	
	
	@Override
	public String toString() {
		Map<String,String> attr = this.getAttributes() ;
		StringBuilder eventXML = new StringBuilder("\t<event ");
		for (Map.Entry<String, String> entry : attr.entrySet()) {
			eventXML.append(entry.getKey());
			eventXML.append("=\"");
			eventXML.append(entry.getValue());
			eventXML.append("\" ");
		}
		eventXML.append(" />");
		return eventXML.toString();
	} 

}
