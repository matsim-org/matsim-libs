package playground.ciarif.retailers.data;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;

public class Consumer {
	
	private final String id;
	private final Person person;
	private Id<RetailZone> rzId;
	private Map<Id<ActivityFacility>,ActivityFacility> shoppingFacilities = new TreeMap<>();  
	private ActivityFacility shoppingFacility;
	
	public Consumer (int id, Person person, Id<RetailZone> rzId) {
		this.person = person;
		this.id = Integer.toString(id);
		this.rzId = rzId;
	}

	public String getId() {
		return this.id;
	}

	public Person getPerson() {
		return this.person;
	}

	public Id<RetailZone> getRzId() {
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
