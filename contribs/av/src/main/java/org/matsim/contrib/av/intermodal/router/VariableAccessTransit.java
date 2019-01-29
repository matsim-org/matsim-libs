package org.matsim.contrib.av.intermodal.router;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

public class VariableAccessTransit implements Provider<RoutingModule> {

	private final TransitRouter transitRouter;

	private final Scenario scenario;

	private final RoutingModule transitWalkRouter;
	private final LeastCostPathCalculator routeAlgo;

	@Inject
    VariableAccessTransit(@Named("variableAccess") TransitRouter transitRouter, Scenario scenario, @Named(TransportMode.transit_walk) RoutingModule transitWalkRouter, LeastCostPathCalculatorFactory routeAlgoF, Map<String,TravelTime> travelTimes) {
		this.transitRouter = transitRouter;
		this.scenario = scenario;
		this.transitWalkRouter = transitWalkRouter;
		TravelTime travelTime = travelTimes.get(TransportMode.car);
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

		this.routeAlgo = routeAlgoF.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
	}

	@Override
	public RoutingModule get() {
		return new VariableAccessTransitRouterWrapper(transitRouter,
					scenario.getTransitSchedule(),
					scenario.getNetwork(),
					transitWalkRouter, routeAlgo);
	}
}
