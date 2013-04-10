package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;

public interface ModeRouteDecisionMaker extends DecisionMaker {

	//Methods
	public List<? extends PlanElement> decideModeRoute(double time, Id startFacilityId, Id endFacilityId);

}
