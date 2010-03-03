package playground.ciarif.retailers.utils;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.PlansCalcRoute;


public class ReRoutePersons {
	
	private final static Logger log = Logger.getLogger(ReRoutePersons.class);

	
	public void run (Map<Id,ActivityFacilityImpl> movedFacilities, Network network, Map<Id, PersonImpl> persons,PlansCalcRoute pcrl, ActivityFacilities facilities){ 
		log.info("movedFacilities= " + movedFacilities);
		int counterPlans = 0;
		int counterPersons = 0;
		for (PersonImpl p : persons.values()) {
			boolean routeIt = false;
			for (Plan plan:p.getPlans()) {
								
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
					 counterPlans = counterPlans+1;
				}
			}
			if (routeIt) {
				log.info("reRouting person: " + p.getId());
				pcrl.run(p);
				counterPersons = counterPersons+1;
			}	
		}
		log.info("The program modified " +  counterPlans + " plans");
		log.info("The program re-routed " +  counterPersons + " persons who were shopping in moved facilities");
	}
}
