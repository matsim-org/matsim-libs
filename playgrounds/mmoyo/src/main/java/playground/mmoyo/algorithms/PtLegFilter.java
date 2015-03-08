package playground.mmoyo.algorithms;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.analysis.filters.population.PersonFilter;

public class PtLegFilter implements PersonFilter {
	
	@Override
	public boolean judge(Person person) {
		for (Plan plan : person.getPlans()){
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg) {
					Leg leg = (Leg)pe;
					if(leg.getMode().equals(TransportMode.pt)){
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void count() {
	
	}

	@Override
	public int getCount() {
		return 0;
	}

	@Override
	public void run(Person person) {
		
	}

}
