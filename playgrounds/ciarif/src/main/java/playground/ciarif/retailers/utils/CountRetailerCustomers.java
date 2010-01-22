package playground.ciarif.retailers.utils;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;

import playground.ciarif.retailers.data.Retailer;

public class CountRetailerCustomers {
	
	private Retailer retailer;
	
	public CountRetailerCustomers (Retailer retailer) {
		this.retailer = retailer;
	}

	public int countCustomers(Map<Id,? extends Person> persons) {
		// TODO Auto-generated method stub
		int customersCount = 0;
		for (Person p:persons.values()) {
			
			for (PlanElement pe2 : p.getSelectedPlan().getPlanElements()) {
				
				if (pe2 instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe2;
					Id f_Id = act.getFacilityId();
					
					if (act.getType().equals("shop") && this.retailer.getFacilities().containsKey(f_Id)) {
							
						customersCount = customersCount+1;
					}
				}
			}
		}
		return customersCount;
	}

}
