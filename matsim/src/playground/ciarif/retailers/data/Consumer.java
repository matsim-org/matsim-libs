package playground.ciarif.retailers.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;

public class Consumer {
	
	private final Id id;
	private final Person person;
	private Id rzId;
	private ActivityFacility shoppingFacility;
	
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
	public void setShoppingFacility(ActivityFacility af){
		this.shoppingFacility = af;
	}
	
	public ActivityFacility getShoppingFacility(){
		return this.shoppingFacility;
	}
}
