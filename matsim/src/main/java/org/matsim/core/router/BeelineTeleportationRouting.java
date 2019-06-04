package org.matsim.core.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import javax.inject.Inject;
import javax.inject.Provider;

class BeelineTeleportationRouting implements Provider<RoutingModule> {

	private final PlansCalcRouteConfigGroup.ModeRoutingParams params;

	public BeelineTeleportationRouting(PlansCalcRouteConfigGroup.ModeRoutingParams params) {
		this.params = params;
	}

	@Inject
	private Scenario scenario ;

	@Override
	public RoutingModule get() {
		return DefaultRoutingModules.createTeleportationRouter(params.getMode(), scenario, params);
	}
}
