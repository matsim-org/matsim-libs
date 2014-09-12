package playground.sergioo.passivePlanning2012.core.router;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.old.LegRouter;

public class EmptyLegRouter implements LegRouter {

	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct,
			Activity toAct, double depTime) {
		if(leg.getMode().equals("empty"))
			return leg.getTravelTime();
		return -1;
	}


}
