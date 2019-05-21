package org.matsim.core.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
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

public class NetworkRoutingProvider implements Provider<RoutingModule> {
	private static final Logger log = Logger.getLogger( NetworkRoutingProvider.class ) ;
	
	private final String routingMode;
	@Inject
    Map<String, TravelTime> travelTimes;

	@Inject
	Map<String, TravelDisutilityFactory> travelDisutilityFactories;

	@Inject
	SingleModeNetworksCache singleModeNetworksCache;

	@Inject
	PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

	@Inject
    Network network;

	@Inject
    PopulationFactory populationFactory;

	@Inject
    LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	@Inject
	Scenario scenario ;
	
	/**
	 * This is the older (and still more standard) constructor, where the routingMode and the resulting mode were the
	 * same.
	 *
	 * @param mode
	 */
	public NetworkRoutingProvider(String mode) {
		this( mode, mode ) ;
	}
	
	/**
	 * The effect of this constructor is a router configured for "routingMode" will be used for routing, but the route
	 * will then have the mode "mode".   So one can, for example, have an uncongested and a congested within-day router,
	 * for travellers who first might be unaware, but then switch on some help, and the both produce a route of type "car".
	 *
	 * @param mode
	 * @param routingMode
	 */
	public NetworkRoutingProvider(String mode, String routingMode ) {
//		log.setLevel(Level.DEBUG);
		
		this.mode = mode;
		this.routingMode = routingMode ;
	}

	private final String mode;

	@Override
	public RoutingModule get() {
		log.debug( "requesting network routing module with routingMode="
						   + routingMode + ";\tmode=" + mode) ;
		
		// the network refers to the (transport)mode:
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

		// the travel time & disutility refer to the routing mode:
		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactories.get(routingMode);
		if (travelDisutilityFactory == null) {
			throw new RuntimeException("No TravelDisutilityFactory bound for mode "+routingMode+".");
		}
		TravelTime travelTime = travelTimes.get(routingMode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode "+routingMode+".");
		}
		LeastCostPathCalculator routeAlgo =
				leastCostPathCalculatorFactory.createPathCalculator(
						filteredNetwork,
						travelDisutilityFactory.createTravelDisutility(travelTime),
						travelTime);

		// the following again refers to the (transport)mode, since it will determine the mode of the leg on the network:
		if ( plansCalcRouteConfigGroup.isInsertingAccessEgressWalk() ) {
			return DefaultRoutingModules.createAccessEgressNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo,
					plansCalcRouteConfigGroup, scenario ) ;
		} else {
			return DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, filteredNetwork, routeAlgo);
		}
	}
}
