package playground.ciarif.retailers.utils;


import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;

public class CountFacilityCustomers {
	
	private Controler controler;
	private ActivityFacilityImpl facility;

	public CountFacilityCustomers(ActivityFacilityImpl facility,Controler controler) {
		this.controler=controler;
		this.facility=facility;
	}

	public int countCustomers() {
		int customersCount = 0;
		 for (Person p: controler.getPopulation().getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if (facility.equals(act.getFacilityId())) {
						customersCount=customersCount+1;
					}
				}
			}
		}
		return customersCount;
	}
}
	

