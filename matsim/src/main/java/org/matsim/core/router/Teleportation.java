package org.matsim.core.router;

import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;

class Teleportation implements Provider<RoutingModule> {

	private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

	public Teleportation(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
		this.params = params;
	}

	@Inject
	private PopulationFactory populationFactory;

	@Override
	public RoutingModule get() {
		return DefaultRoutingModules.createTeleportationRouter(params.getMode(), populationFactory, params);
	}
}
