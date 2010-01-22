package playground.ciarif.retailers.utils;


import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;

public class CountFacilityCustomers {
	
	private Map<Id,? extends Person> persons;

	public CountFacilityCustomers(Map<Id,? extends Person> persons) {
		this.persons=persons;
	}

	public int countCustomers(ActivityFacilityImpl facility) {
		int customersCount = 0;
		 for (Person p: persons.values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if (act.getType().equals("shop") && act.getFacilityId().equals(facility.getId())){
						customersCount=customersCount+1;
					}		
				}
			}
		}
	
	return customersCount;
	
	}
}
	

