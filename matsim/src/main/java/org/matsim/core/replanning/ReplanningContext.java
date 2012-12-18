package org.matsim.core.replanning;

import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public interface ReplanningContext {

	TripRouterFactory getTripRouterFactory();

	TravelDisutility getTravelCostCalculator();

	TravelTime getTravelTimeCalculator();

}
