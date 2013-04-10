package playground.sergioo.passivePlanning2012.core.population.decisionMakers;

import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.router.TripRouter;

import playground.sergioo.passivePlanning2012.core.population.decisionMakers.types.ModeRouteDecisionMaker;
import playground.sergioo.passivePlanning2012.core.router.TripUtils;

public class SimpleModeRouteDecisionMaker implements ModeRouteDecisionMaker {

	//Attributes
	private final Set<String> modes;
	private final TripRouter tripRouter;
	private final ActivityFacilities facilities;
	
	//Methods
	public SimpleModeRouteDecisionMaker(Set<String> modes, TripRouter tripRouter, ActivityFacilities facilities) {
		this.modes = modes;
		this.tripRouter = tripRouter;
		this.facilities = facilities;
	}
	@Override
	public List<? extends PlanElement> decideModeRoute(double time, Id startFacilityId, Id endFacilityId) {
		double lessTime = Double.MAX_VALUE;
		List<? extends PlanElement> shortestTrip = null;
		for (String mode : modes) {
			List<? extends PlanElement> trip = tripRouter.calcRoute(mode, facilities.getFacilities().get(startFacilityId), facilities.getFacilities().get(endFacilityId), time, null);
			double timeT = TripUtils.calcTravelTime(trip);
			if(timeT<lessTime) {
				lessTime = timeT;
				shortestTrip = trip;
			}
		}
		return shortestTrip;
	}
	
}
