package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NetworkRouting implements Provider<RoutingModule> {

	@Inject
    Map<String, TravelTime> travelTimes;

	@Inject
	Map<String, TravelDisutilityFactory> travelDisutilityFactories;

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

	public NetworkRouting(String mode) {
		this.mode = mode;
	}

	private String mode;

	@Override
	public RoutingModule get() {
		Network filteredNetwork = null;

		// Ensure this is not performed concurrently by multiple threads!
		synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {
			filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
			if (filteredNetwork == null) {
				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
				Set<String> modes = new HashSet<>();
				modes.add(mode);
				filteredNetwork = NetworkUtils.createNetwork();
				filter.filter(filteredNetwork, modes);
				this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
			}
		}

		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactories.get(mode);
		if (travelDisutilityFactory == null) {
			throw new RuntimeException("No TravelDisutilityFactory bound for mode "+mode+".");
		}
		TravelTime travelTime = travelTimes.get(mode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode "+mode+".");
		}
		LeastCostPathCalculator routeAlgo =
				leastCostPathCalculatorFactory.createPathCalculator(
						filteredNetwork,
						travelDisutilityFactory.createTravelDisutility(travelTime),
						travelTime);

		if ( plansCalcRouteConfigGroup.isInsertingAccessEgressWalk() ) {
			return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo,
					plansCalcRouteConfigGroup) ;
		} else {
			return DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo);
		}
	}
}
