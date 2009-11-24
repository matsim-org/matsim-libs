package playground.ciarif.retailers.data;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;

public class Consumer {
	
	private final Id id;
	private final Person person;
	private Id rzId;
	private ActivityFacilityImpl shoppingFacility;
	
	public Consumer (int id, Person person, Id rzId) {
		this.person = person;
		this.id = new IdImpl (id);
		this.rzId = rzId;
	}

	public Id getId() {
		return this.id;
	}

	public Person getPerson() {
		return this.person;
	}

	public Id getRzId() {
		return this.rzId;
	}
	public void setShoppingFacility(ActivityFacilityImpl af){
		this.shoppingFacility = af;
	}
	
	public ActivityFacilityImpl getShoppingFacility(){
		return this.shoppingFacility;
	}
}
