package org.matsim.contrib.drt.taas;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class TaasDrtRouteCreator implements DefaultMainLegRouter.RouteCreator {
	static public final String REQUEST_TYPE = "requestType";

	private final DrtConfigGroup drtCfg;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final TaasRouteParameters parameters;

	public TaasDrtRouteCreator(DrtConfigGroup drtCfg, Network modalNetwork,
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory, TravelTime travelTime,
			TravelDisutilityFactory travelDisutilityFactory, TaasRouteParameters parameters) {
		this.drtCfg = drtCfg;
		this.travelTime = travelTime;
		this.parameters = parameters;
		router = leastCostPathCalculatorFactory.createPathCalculator(modalNetwork,
				travelDisutilityFactory.createTravelDisutility(travelTime), travelTime);
	}

	public Route createRoute(double departureTime, Link accessActLink, Link egressActLink, Person person,
			Attributes tripAttributes, RouteFactories routeFactories) {
		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
				router, travelTime);
		double unsharedRideTime = unsharedPath.getTravelTime();// includes first & last link
		double unsharedDistance = VrpPaths.calcDistance(unsharedPath);// includes last link

		String requestType = (String) person.getAttributes().getAttribute(REQUEST_TYPE);

		final double maxTravelTime;
		final double maxRideDuration = Double.POSITIVE_INFINITY;
		final double maxWaitTime;

		if (requestType.equals("passenger")) {
			maxTravelTime = unsharedRideTime + parameters.passengerMaxDelay;
			maxWaitTime = parameters.passengerMaxWaitTime;
		} else if (requestType.equals("parcel")) {
			maxTravelTime = parameters.parcelLatestDeliveryTime - departureTime;
			maxWaitTime = maxTravelTime;
		} else {
			throw new IllegalStateException();
		}

		DrtRoute route = routeFactories.createRoute(DrtRoute.class, accessActLink.getId(), egressActLink.getId());
		route.setDistance(unsharedDistance);
		route.setTravelTime(maxTravelTime);
		route.setMaxRideTime(maxRideDuration);
		route.setDirectRideTime(unsharedRideTime);
		route.setMaxWaitTime(maxWaitTime);

		if (this.drtCfg.storeUnsharedPath) {
			route.setUnsharedPath(unsharedPath);
		}

		return route;
	}

	static public record TaasRouteParameters(double passengerMaxWaitTime, double passengerMaxDelay,
			double parcelLatestDeliveryTime) {
	}
}
