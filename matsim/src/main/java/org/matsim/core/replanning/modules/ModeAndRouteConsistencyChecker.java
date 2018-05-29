package org.matsim.core.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripStructureUtils;

public class ModeAndRouteConsistencyChecker implements PlanStrategyModule {
	@Override public void prepareReplanning(ReplanningContext replanningContext) { }
	
	@Override public void handlePlan(Plan plan) {
		for (Leg leg : TripStructureUtils.getLegs(plan)) {
			if (leg.getRoute() instanceof NetworkRoute) {
				switch ( leg.getMode() ) {
					case TransportMode.car:
					case TransportMode.bike:
					case TransportMode.walk:
						break;
					default:
						Logger.getLogger(this.getClass()).warn( "route is of type=" + leg.getRoute().getClass() ) ;
						Logger.getLogger(this.getClass()).warn( "mode=" + leg.getMode() ) ;
						throw new RuntimeException("inconsistent");
				}
			}
		}
	}
	
	@Override public void finishReplanning() { }
}
