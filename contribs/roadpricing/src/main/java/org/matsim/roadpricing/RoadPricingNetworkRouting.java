package org.matsim.roadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom router which routes on the "car" network, but uses a custom 
 * {@link TravelDisutility} which does *not* contain extra link cost.
 * The *regular* "car" router gets a {@link TravelDisutility} which makes 
 * "car" prohibitively expensive, and {@link PlansCalcRouteWithTollOrNot} uses 
 * this setup to calculate a best response plan (with paid toll or not).
 *
 * TODO I'm sure this can be made easier and more flexible (michaz 2016)
 */
class RoadPricingNetworkRouting implements Provider<RoutingModule> {

	@Inject private Map<String, TravelTime> travelTimes;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactory;
	@Inject private PlansCalcRouteConfigGroup plansCalcRouteConfigGroup ;
	@Inject private Network network;
	@Inject private PopulationFactory populationFactory;
	@Inject private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	@Inject private Scenario scenario ;

	private Network filteredNetwork;

	@Override
	public RoutingModule get() {
		if (filteredNetwork == null){
			TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
			Set<String> modes = new HashSet<>();
			modes.add(TransportMode.car);
			filteredNetwork = NetworkUtils.createNetwork();
			filter.filter(filteredNetwork, modes);
		}
		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactory.get(PlansCalcRouteWithTollOrNot.CAR_WITH_PAYED_AREA_TOLL);
		TravelTime travelTime = travelTimes.get(TransportMode.car);
		LeastCostPathCalculator routeAlgo =
			  leastCostPathCalculatorFactory.createPathCalculator(
				    filteredNetwork,
				    travelDisutilityFactory.createTravelDisutility(travelTime),
				    travelTime);
		if ( plansCalcRouteConfigGroup.isInsertingAccessEgressWalk() ) {
			return DefaultRoutingModules.createAccessEgressNetworkRouter(TransportMode.car, routeAlgo, scenario, filteredNetwork );
		} else {
			return DefaultRoutingModules.createPureNetworkRouter(TransportMode.car, populationFactory,
				  filteredNetwork, routeAlgo);
		}
		// yyyyyy not so great that this differentiation is here; need to push it down a bit (again). kai, feb'2016
	}
}
