package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.router.TripRouter;

public interface ModeRouteDecisionMaker extends DecisionMaker {

	//Methods
	public List<? extends PlanElement> decideModeRoute(double time, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, TripRouter tripRouter);

}
