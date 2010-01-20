package playground.ciarif.retailers.utils;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.PlansCalcRoute;


public class ReRoutePersons {
	
	private final static Logger log = Logger.getLogger(ReRoutePersons.class);
	//private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
	
	public void run (Map<Id,ActivityFacilityImpl> movedFacilities, Network network, Map<Id,? extends Person> persons,PlansCalcRoute pcrl, ActivityFacilities facilities){ 
		int counter = 0;
		for (Person p : persons.values()) {
			for (Plan plan:p.getPlans()) {
				 
				boolean routeIt = false;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;
						if (movedFacilities.containsKey(act.getFacilityId())) { 
							act.setLinkId(((ActivityFacilityImpl) facilities.getFacilities().get((act.getFacilityId()))).getLinkId());
							routeIt = true;
						}
					}
				}
				
				if (routeIt) {
					pcrl.run(plan);
					counter = counter+1;
				}
			}
			
		}
		log.info("The program re-routed " +  counter + " persons who were shopping in moved facilities");
	}
}
