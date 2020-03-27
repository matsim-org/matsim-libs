package org.matsim.pt.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class DefaultTransitPassengerRouteFactory implements RouteFactory {
	@Override
	public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		return new DefaultTransitPassengerRoute(startLinkId, endLinkId);
	}

	@Override
	public String getCreatedRouteType() {
		return DefaultTransitPassengerRoute.ROUTE_TYPE;
	}
}