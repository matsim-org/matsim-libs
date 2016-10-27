package org.matsim.contrib.carsharing.manager.demand.membership;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
/** 
 * @author balac
 */
public class MembershipContainer {
	
	private Map<Id<Person>, PersonMembership> perPersonMemberships = new HashMap<Id<Person>, PersonMembership>();
	
	public void addPerson(String personId, PersonMembership personMembership) {
		
		Id<Person> personId2 = Id.createPersonId(personId);
		this.perPersonMemberships.put(personId2, personMembership);
	}
	
	public Map<Id<Person>, PersonMembership> getPerPersonMemberships() {
		return perPersonMemberships;
	}
}
