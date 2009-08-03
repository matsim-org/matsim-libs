package playground.ciarif.retailers.utils;

import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;

public class ReRoutePersons {
	
	private PlansCalcRoute pcrl = null;
	private final FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
	private final PreProcessLandmarks preprocess = new PreProcessLandmarks(timeCostCalc);
	//private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
	
	public void run (Map<Id,ActivityFacility> movedFacilities, NetworkLayer network, Map<Id,PersonImpl> persons){ 
		
			
		pcrl = new PlansCalcRoute(network,timeCostCalc, timeCostCalc, new AStarLandmarksFactory(preprocess));
		int counter = 0;
		for (PersonImpl p : persons.values()) {
			
			PlanImpl plan = p.getSelectedPlan(); 
			boolean routeIt = false;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl) pe;
					if (movedFacilities.containsKey(act.getFacilityId())) { //TODO use here another movedFacilities object, this one very 
						// likely contains too much persons in it!!!!
						act.setLink(act.getFacility().getLink());
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
}
