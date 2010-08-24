package playground.mmoyo.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.LegImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**filters a population leaving only persons using some transit routes*/
public class TrRouteFilter4Plan {

	public Population filterPlan (Population population, List<Id> trRoutesIds ){
		List<Id> personsToDel = new ArrayList<Id>();
		for (Person person : population.getPersons().values()){
			boolean keepPerson = false;
			for (Plan plan : person.getPlans()){
				List<PlanElement> planElements = plan.getPlanElements();
				for (PlanElement planElement : planElements){
					if (planElement instanceof LegImpl) {
						LegImpl leg = (LegImpl)planElement;
						if (leg.getRoute()!= null && (leg.getRoute() instanceof ExperimentalTransitRoute) ){
							ExperimentalTransitRoute expTrRoute = ((ExperimentalTransitRoute)leg.getRoute());
							keepPerson =  keepPerson || trRoutesIds.contains(expTrRoute.getRouteId()); 
						}
					}
				}
			}
			if (!keepPerson) personsToDel.add(person.getId());
		}
		
		//delete persons that do not use the routes
		for (Id personId : personsToDel){
			population.getPersons().remove(personId);
		}
		
		return population;
	}
	
	public static void main(String[] args) {

	}

}
