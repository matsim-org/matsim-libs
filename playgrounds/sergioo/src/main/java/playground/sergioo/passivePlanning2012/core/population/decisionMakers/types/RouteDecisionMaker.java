package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Route;

public interface RouteDecisionMaker extends DecisionMaker {

	//Methods
	public Route decideRoute(double time, Id startFacilityId, Id endFacilityId);

}
