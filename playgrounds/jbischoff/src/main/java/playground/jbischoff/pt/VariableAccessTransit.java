package playground.jbischoff.pt;

import com.google.inject.name.Named;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingModule;
import org.matsim.pt.router.TransitRouter;

import javax.inject.Inject;
import javax.inject.Provider;

public class VariableAccessTransit implements Provider<RoutingModule> {

	private final TransitRouter transitRouter;

	private final Scenario scenario;

	private final RoutingModule transitWalkRouter;

	@Inject
    VariableAccessTransit(TransitRouter transitRouter, Scenario scenario, @Named(TransportMode.transit_walk) RoutingModule transitWalkRouter) {
		this.transitRouter = transitRouter;
		this.scenario = scenario;
		this.transitWalkRouter = transitWalkRouter;
	}

	@Override
	public RoutingModule get() {
		return new VariableAccessTransitRouterWrapper(transitRouter,
					scenario.getTransitSchedule(),
					scenario.getNetwork(),
					transitWalkRouter);
	}
}
