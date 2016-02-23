package org.matsim.roadpricing;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom router which routes on the "car" network,
 * but uses a custom TravelDisutility which does *not* contain extra link cost.
 * The *regular* "car" router gets a TravelDisutility which makes "car" prohibitively
 * expensive, and PlansCalcRouteWithTollOrNot uses this setup to calculate a best response
 * plan (with paid toll or not).
 *
 * I'm sure this can be made easier and more flexible.
 * michaz 2016
 */
public class RoadPricingNetworkRouting implements Provider<RoutingModule> {

	@Inject
    Map<String, TravelTime> travelTimes;

	@Inject
	Map<String, TravelDisutilityFactory> travelDisutilityFactory;

	@Inject
	SingleModeNetworksCache singleModeNetworksCache;

	@Inject
    PlanCalcScoreConfigGroup planCalcScoreConfigGroup;
	
	@Inject PlansCalcRouteConfigGroup plansCalcRouteConfigGroup ;

	@Inject
    Network network;

	@Inject
    PopulationFactory populationFactory;

	@Inject
    LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	@Override
	public RoutingModule get() {
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
		Set<String> modes = new HashSet<>();
		modes.add(TransportMode.car);
		Network filteredNetwork = NetworkUtils.createNetwork();
		filter.filter(filteredNetwork, modes);
		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactory.get(PlansCalcRouteWithTollOrNot.CAR_WITH_PAYED_AREA_TOLL);
		TravelTime travelTime = travelTimes.get(TransportMode.car);
		LeastCostPathCalculator routeAlgo =
				leastCostPathCalculatorFactory.createPathCalculator(
						filteredNetwork,
						travelDisutilityFactory.createTravelDisutility(travelTime),
						travelTime);
		if ( plansCalcRouteConfigGroup.isInsertingAccessEgressWalk() ) {
			return DefaultRoutingModules.createAccessEgressNetworkRouter(TransportMode.car, populationFactory,
					filteredNetwork, routeAlgo, plansCalcRouteConfigGroup);
		} else {
			return DefaultRoutingModules.createPureNetworkRouter(TransportMode.car, populationFactory,
					filteredNetwork, routeAlgo);
		}
		// yyyyyy not so great that this differentiation is here; need to push it down a bit (again). kai, feb'2016
	}
}
