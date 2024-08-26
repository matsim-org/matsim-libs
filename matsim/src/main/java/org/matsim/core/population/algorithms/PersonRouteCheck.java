package org.matsim.core.population.algorithms;

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

		if (!(route instanceof NetworkRoute netRoute)) {
			return;
		}

		boolean allLinksHaveLegMode = netRoute.getLinkIds().stream()
											  .map(id -> network.getLinks().get(id))
											  .allMatch(link -> link.getAllowedModes().contains(leg.getMode()));

		if (!allLinksHaveLegMode) {
			leg.setRoute(null);
		}
	}
}
