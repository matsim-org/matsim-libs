/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressMultimodalTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.router.multimodal;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import playground.ivt.utils.SoftCache;

import javax.inject.Provider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author thibautd
 */
public class AccessEgressMultimodalTripRouterModule extends AbstractModule {
	private final Scenario scenario;
	private final Map<String, TravelTime> multimodalTravelTimes;

	private final Map<String, Network> multimodalSubNetworks = new HashMap<String, Network>();
	private final Map<String, LeastCostPathCalculatorFactory> multimodalFactories = new HashMap<String, LeastCostPathCalculatorFactory>();
	private final ConcurrentMap<String, SoftCache<Tuple<Node, Node>, Path>> caches = new ConcurrentHashMap<String, SoftCache<Tuple<Node, Node>, Path>>();
	private final Map<String, TravelDisutilityFactory> disutilityFactories = new HashMap<>();


	public AccessEgressMultimodalTripRouterModule(
			final Scenario scenario,
			final Map<String, TravelTime> multimodalTravelTimes ) {
		this.scenario = scenario;
		this.multimodalTravelTimes = multimodalTravelTimes;
	}
	
	@Override
	public void install() {
		final Network network = this.scenario.getNetwork();

        final MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        final Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		for (String mode : simulatedModes) {
			addRoutingModuleBinding(mode)
					.toProvider(
							new AccessEgressNetworkBasedTeleportationRoutingModuleProvider(
									mode,
									network));
			addTravelTimeBinding(mode).toInstance(multimodalTravelTimes.get(mode));
			if (disutilityFactories.containsKey(mode)) {
				addTravelDisutilityFactoryBinding(mode).toInstance(disutilityFactories.get(mode));
			} else {
				addTravelDisutilityFactoryBinding(mode).to(carTravelDisutilityFactoryKey());
			}
		}
	}

	private class AccessEgressNetworkBasedTeleportationRoutingModuleProvider implements Provider<RoutingModule> {
		private final String mode;
		private final Network network;

		@Inject
		private Map<String, TravelDisutilityFactory> travelDisutilityFactories = null;

		private AccessEgressNetworkBasedTeleportationRoutingModuleProvider(String mode, Network network) {
			this.mode = mode;
			this.network = network;
		}

		@Override
		public RoutingModule get() {

			final TravelTime travelTime = multimodalTravelTimes.get(mode);
			if (travelTime == null) {
				throw new RuntimeException("No travel time object was found for mode " + mode + "! Aborting.");
			}

			final Network subNetwork = getSubnetwork(network, mode);

			/*
			 * We cannot use the travel disutility object from the routingContext since it
			 * has not been created for the modes used here.
			 */

			final TravelDisutility travelDisutility =
							 	travelDisutilityFactories.get( mode ).createTravelDisutility(
										travelTime);
			final TravelDisutility nonPersonnalizableDisutility =
					new TravelDisutility() {
						private final Person dummy = PopulationUtils.createPerson(Id.create("dummy", Person.class));
						@Override
						public double getLinkTravelDisutility(
								final Link link,
								final double time,
								final Person person,
								final Vehicle vehicle) {
							return travelDisutility.getLinkTravelDisutility(
									link,
									time,
									// This is fine with the caching approach,
									// and is necessary for the AStar heuristic
									// to be valid
									dummy,
									vehicle );
						}

						@Override
						public double getLinkMinimumTravelDisutility(final Link link) {
							// default uses freespeed, which is worthless for slow modes.
							// This here is fine with Christoph Dobler's Walk and Bike
							// disutilities, as they are not travel time dependent and
							// we anyway do not use the person in routing but as a post-processing.
							// But this remains awful (basically, the problem is
							// how to pre-process when one has personnalizable
							// travel times?)
							return travelDisutility.getLinkTravelDisutility( link , 0 , dummy , null );
						}
					};
			// we use the "non-personnalizable" version for routing,
			// and the personnalizable only for path adaptation
			final LeastCostPathCalculator routeAlgo =
				new CachingLeastCostPathAlgorithmWrapper(
						getCache( mode ),
						travelTime,
						travelDisutility,
						getLeastCostPathCalulatorFactory(
							mode,
							subNetwork,
							nonPersonnalizableDisutility ).createPathCalculator(
								subNetwork,
								nonPersonnalizableDisutility,
								travelTime) );
			final double crowFlyDistanceFactor = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(mode).getBeelineDistanceFactor();
			final double crowFlySpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get( mode );

			return new AccessEgressNetworkBasedTeleportationRoutingModule(
									mode,
									subNetwork,
									crowFlyDistanceFactor,
									crowFlySpeed,
									routeAlgo );
		}
	}

	private SoftCache<Tuple<Node, Node>, Path> getCache(final String mode) {
		return MapUtils.getArbitraryObject(
				mode,
				caches,
				new MapUtils.Factory<SoftCache<Tuple<Node, Node>, Path>>() {
					@Override
					public SoftCache<Tuple<Node, Node>, Path> create() {
						return new SoftCache<Tuple<Node, Node>, Path>();
					}
				}
				);
	}

	private LeastCostPathCalculatorFactory getLeastCostPathCalulatorFactory(
			final String mode,
			final Network subNetwork,
			final TravelDisutility travelDisutility) {
		if ( multimodalFactories.containsKey( mode ) ) return multimodalFactories.get( mode );

		// TODO: make implementation configurable
		final LeastCostPathCalculatorFactory factory =
			new FastAStarLandmarksFactory(
					subNetwork,
					travelDisutility );

		multimodalFactories.put( mode , factory );
		return factory;
	}

	private Network getSubnetwork(final Network network,final String mode) {
		Network subNetwork = multimodalSubNetworks.get(mode);
		
		if (subNetwork == null) {
			subNetwork = NetworkImpl.createNetwork();
			final Set<String> restrictions = new HashSet<String>();
			restrictions.add(mode);
			final TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
			networkFilter.filter(subNetwork, restrictions);
			this.multimodalSubNetworks.put(mode, subNetwork);
		}
		
		return subNetwork;
	}

	public void setDisutilityFactoryForMode(
			final String mode,
			final TravelDisutilityFactory factory ) {
		this.disutilityFactories.put( mode , factory );
	}
}

