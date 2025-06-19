package org.matsim.contrib.ev.routing;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TruckEvNetworkRoutingProvider implements Provider<RoutingModule> {
	private static final Logger log = LogManager.getLogger(TruckEvNetworkRoutingProvider.class);

	private final String routingMode;
	@Inject
	Map<String, TravelTime> travelTimes;

	@Inject
	Map<String, TravelDisutilityFactory> travelDisutilityFactories;

	@Inject
	SingleModeNetworksCache singleModeNetworksCache;

	@Inject
	Config config;

	@Inject
	RoutingConfigGroup routingConfigGroup;

	@Inject
	Network network;

	@Inject
	PopulationFactory populationFactory;

	@Inject
	LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	@Inject
	ElectricFleetSpecification electricFleetSpecification;

	@Inject
	ChargingInfrastructureSpecification chargingInfrastructureSpecification;

	@Inject
	DriveEnergyConsumption.Factory driveConsumptionFactory;

	@Inject
	AuxEnergyConsumption.Factory auxConsumptionFactory;

	@Inject
	Scenario scenario;

	@Inject
	TimeInterpretation timeInterpretation;

	@Inject
	MultimodalLinkChooser multimodalLinkChooser;

	@Inject
	@Named(TransportMode.walk)
	RoutingModule walkRouter;

	/**
	 * This is the older (and still more standard) constructor, where the routingMode and the resulting mode were the
	 * same.
	 *
	 * @param mode
	 */
	public TruckEvNetworkRoutingProvider(String mode) {
		this(mode, mode);
	}

	/**
	 * The effect of this constructor is a router configured for "routingMode" will be used for routing, but the route
	 * will then have the mode "mode".   So one can, for example, have an uncongested and a congested within-day router,
	 * for travellers who first might be unaware, but then switch on some help, and the both produce a route of type "car".
	 *
	 * @param mode
	 * @param routingMode
	 */
	public TruckEvNetworkRoutingProvider(String mode, String routingMode) {
		this.mode = mode;
		this.routingMode = routingMode;
	}

	private final String mode;

	@Override
	public RoutingModule get() {
		log.debug("requesting network routing module with routingMode=" + routingMode + ";\tmode=" + mode);

		// the network refers to the (transport)mode:
		Network filteredNetwork;

		// Ensure this is not performed concurrently by multiple threads!
		synchronized (this.singleModeNetworksCache.getSingleModeNetworksCache()) {

			filteredNetwork = this.singleModeNetworksCache.getSingleModeNetworksCache().get(mode);
			if (filteredNetwork == null) {
				TransportModeNetworkFilter filter = new TransportModeNetworkFilter(network);
				Set<String> modes = new HashSet<>();
				modes.add(mode);
				filteredNetwork = NetworkUtils.createNetwork(config);
				filter.filter(filteredNetwork, modes);
				this.singleModeNetworksCache.getSingleModeNetworksCache().put(mode, filteredNetwork);
			}
		}

		// the travel time & disutility refer to the routing mode:
		TravelDisutilityFactory travelDisutilityFactory = this.travelDisutilityFactories.get(routingMode);
		if (travelDisutilityFactory == null) {
			throw new RuntimeException("No TravelDisutilityFactory bound for mode " + routingMode + ".");
		}
		TravelTime travelTime = travelTimes.get(routingMode);
		if (travelTime == null) {
			throw new RuntimeException("No TravelTime bound for mode " + routingMode + ".");
		}

		LeastCostPathCalculator routeAlgo = leastCostPathCalculatorFactory.createPathCalculator(filteredNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);

		// the following again refers to the (transport)mode, since it will determine the mode of the leg on the network:
		if (!routingConfigGroup.getAccessEgressType().equals(AccessEgressType.accessEgressModeToLink)) {
			throw new IllegalArgumentException("Bushwacking is not currently supported by the EV routing module");
		} else {
			return new TruckEvNetworkRoutingModule(mode, filteredNetwork,
					DefaultRoutingModules.createAccessEgressNetworkRouter(mode, routeAlgo, scenario, filteredNetwork,
						walkRouter, timeInterpretation, multimodalLinkChooser),
					electricFleetSpecification, chargingInfrastructureSpecification, travelTime,
					driveConsumptionFactory, auxConsumptionFactory, EvConfigGroup.get(config));
		}
	}
}
