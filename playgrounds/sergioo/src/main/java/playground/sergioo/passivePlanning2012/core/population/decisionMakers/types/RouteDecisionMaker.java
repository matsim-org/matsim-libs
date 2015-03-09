package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;

public interface RouteDecisionMaker extends DecisionMaker {

	//Methods
	public List<? extends PlanElement> decideRoute(double time, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId, String mode, TripRouter tripRouter);

}
