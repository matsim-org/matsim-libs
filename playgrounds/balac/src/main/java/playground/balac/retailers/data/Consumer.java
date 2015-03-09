package playground.balac.retailers.data;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

public class Consumer {
	
	private final Id<Consumer> id;
	private final Person person;
	private Id rzId;
	private Map<Id<ActivityFacility>,ActivityFacility> shoppingFacilities = new TreeMap<>();  
	private ActivityFacility shoppingFacility;
	
	public Consumer (int id, Person person, Id rzId) {
		this.person = person;
		this.id = Id.create(id, Consumer.class);
		this.rzId = rzId;
	}

	public Id<Consumer> getId() {
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
	
	public void addShoppingFacility (ActivityFacility af) {
		int size = this.shoppingFacilities.size();
		Id<ActivityFacility> id = Id.create(size, ActivityFacility.class);  
		this.shoppingFacilities.put(id, af);
	}
	
	public ActivityFacility getShoppingFacility(){
		return this.shoppingFacility;
	}
	
	public Map<Id<ActivityFacility>,ActivityFacility> getShoppingFacilities(){
		return this.shoppingFacilities;
	}
}
