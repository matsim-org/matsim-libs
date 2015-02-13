package playground.balac.allcsmodestest.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class StartRentalEvent extends Event{

	private final Id<Link> linkId;
	
	private final Id<Person> personId;
	
	private final Id<Vehicle> vehicleId;
	
	public static final String EVENT_TYPE = "Rental Start";

	public StartRentalEvent(double time, Id<Link> linkId, Id<Person> personId, Id<Vehicle> vehicleId) {
		super(time);
		// TODO Auto-generated constructor stub
		this.linkId = linkId;
		this.personId = personId;
		this.vehicleId = vehicleId;
		
	}

	@Override
	public String getEventType() {
		// TODO Auto-generated method stub
		return EVENT_TYPE;
	}
	
	public Id<Link> getLinkId(){
		return this.linkId;
	}
	
	public Id<Person> getPersonId(){
		return this.personId;
	}
	
	public Id<Vehicle> getvehicleId(){
		return this.vehicleId;
	}
	
	

}
