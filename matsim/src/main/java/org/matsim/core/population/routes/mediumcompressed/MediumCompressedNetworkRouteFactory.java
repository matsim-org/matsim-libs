package org.matsim.core.population.routes.mediumcompressed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;

/**
 * @author mrieser / Simunto
 */
public class MediumCompressedNetworkRouteFactory implements RouteFactory {

	@Override
	public NetworkRoute createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		return new MediumCompressedNetworkRoute(startLinkId, endLinkId);
	}

	@Override
	public String getCreatedRouteType() {
		return "links";
	}

}
