package org.matsim.core.population.algorithms;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;

public class PersonRouteCheck implements PersonAlgorithm{
	private final Network network;

	public PersonRouteCheck(Network network) {
		this.network = network;
	}

	@Override
	public void run(Person person) {
		person.getPlans().stream()
			  .flatMap(p -> p.getPlanElements().stream())
			  .filter(pe -> pe instanceof Leg)
			  .map(pe -> (Leg) pe)
			  .forEach(this::resetRouteIfInconsistentModes);
	}

	private void resetRouteIfInconsistentModes(Leg leg) {
		Route route = leg.getRoute();
		if (route == null) {
			return;
		}

		boolean allLinksHaveLegMode = true;

		Link startLinkId = network.getLinks().get( route.getStartLinkId() );
		Link endLinkId = network.getLinks().get( route.getEndLinkId() );
		if ( startLinkId==null || endLinkId==null ) {
			allLinksHaveLegMode = false;
		}
		// (start/endLinkId might also be in the netRoute.getLinkIds below, but better be safe than sorry, and we need the check for non-network-routes anyways.)

		if ( route instanceof NetworkRoute ){
			NetworkRoute netRoute = (NetworkRoute) route;
			for( Id<Link> id : netRoute.getLinkIds() ){
				Link link = network.getLinks().get( id );
				if( link == null || !(link.getAllowedModes().contains( leg.getMode() )) ){
					allLinksHaveLegMode = false;
					break;
				}
			}
		}

		if (!allLinksHaveLegMode) {
			leg.setRoute(null);
		}
	}
}
