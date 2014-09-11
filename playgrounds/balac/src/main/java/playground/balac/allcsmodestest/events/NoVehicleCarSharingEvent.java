package playground.balac.allcsmodestest.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public class NoVehicleCarSharingEvent extends Event{

	public static final String EVENT_TYPE = "no carsharing vehicle";
	
	private final Id linkId;
	
	private final String carsharingType;
	
	public NoVehicleCarSharingEvent(double time, Id linkId, String carsharingType) {
		super(time);
		this.linkId = linkId;
		this.carsharingType = carsharingType;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public Id getLinkId(){
		return this.linkId;
	}
	
	public String getCarsharingType() {
		return this.carsharingType;
	}

}
