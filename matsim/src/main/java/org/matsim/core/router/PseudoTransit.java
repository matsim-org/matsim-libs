package org.matsim.core.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

import javax.inject.Inject;
import javax.inject.Provider;

class PseudoTransit implements Provider<RoutingModule> {

	private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

	public PseudoTransit(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
		this.params = params;
	}

	@Inject
	private Network network;

	@Inject
	private PopulationFactory populationFactory;

	@Inject
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	@Override
	public RoutingModule get() {
		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
				new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow =
				leastCostPathCalculatorFactory.createPathCalculator(
						network,
						ptTimeCostCalc,
						ptTimeCostCalc);
		return DefaultRoutingModules.createPseudoTransitRouter(params.getMode(), populationFactory,
				network, routeAlgoPtFreeFlow, params);
	}
}
