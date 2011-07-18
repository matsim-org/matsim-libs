package playground.mmoyo.utils;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.population.algorithms.PlanAlgorithm;

public class NonPtRouteNullifier implements PlanAlgorithm  {

	@Override
	public void run(final Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (!TransportMode.pt.equals(leg.getMode())) {
					leg.setRoute(null);
				}
			}
		}
	}

}
