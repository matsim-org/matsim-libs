package playground.andreas.intersection.dijkstra;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * A AbstractPersonAlgorithm that calculates and sets the routes of a person's activities using {@link Dijkstra}.
 *
 * @author mrieser, aneumann
 */
public class PlansCalcRouteDijkstra extends PlansCalcRoute {

	Network wrappedNetwork;
	Network originalNetwork;

	public PlansCalcRouteDijkstra(final Network originalNetwork, final Network wrappedNetwork, final TravelCost costCalculator, final TravelTime timeCalculator, final CharyparNagelScoringConfigGroup config) {
		this(originalNetwork, wrappedNetwork, costCalculator, timeCalculator, new FreespeedTravelTimeCost(config));
	}

	@SuppressWarnings("deprecation")
	private PlansCalcRouteDijkstra(final Network originalNetwork, final Network wrappedNetwork, final TravelCost costCalculator, final TravelTime timeCalculator,
			final FreespeedTravelTimeCost freespeedTimeCost) {
		super(wrappedNetwork, new Dijkstra(wrappedNetwork, costCalculator, timeCalculator),
				new Dijkstra(wrappedNetwork, freespeedTimeCost, freespeedTimeCost));
		this.originalNetwork = originalNetwork;
		this.wrappedNetwork = wrappedNetwork;
	}

	/*
	 * Need to override it here, to ensure correct route conversion to wrapped network and vice versa.
	 * That's why I had to change visibility to protected.
	 *
	 * TODO [an] No other transport means are implemented, yet.
	 *
	 * (non-Javadoc)
	 * @see org.matsim.router.PlansCalcRoute#handleCarLeg(org.matsim.population.Leg, org.matsim.population.Act, org.matsim.population.Act, double)
	 */
	@Override
	protected double handleCarLeg(final LegImpl leg, final ActivityImpl fromAct, final ActivityImpl toAct, final double depTime) {
		double travTime = 0;
		Id fromLinkId = fromAct.getLinkId();
		Id toLinkId = toAct.getLinkId();
		if (fromLinkId == null) throw new RuntimeException("fromLink missing.");
		if (toLinkId == null) throw new RuntimeException("toLink missing.");

		Node startNode = this.wrappedNetwork.getNodes().get(fromLinkId);	// start at the end of the "current" link
		Node endNode = this.wrappedNetwork.getNodes().get(toLinkId); // the target is the start of the link

		if (!toLinkId.equals(fromLinkId)) {
			Path path = null;
			// do not drive/walk around, if we stay on the same link
			path = this.getLeastCostPathCalculator().calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

			ArrayList<Node> realRouteNodeList = new ArrayList<Node>();

			for (Node node : path.nodes) {
				realRouteNodeList.add(originalNetwork.getLinks().get(node.getId()).getToNode());
			}

			realRouteNodeList.remove(realRouteNodeList.size() - 1);

			NetworkRouteWRefs wrappedRoute = new NodeNetworkRouteImpl(null, null, wrappedNetwork);
			wrappedRoute.setLinkIds(null, NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(realRouteNodeList)), null);
			wrappedRoute.setTravelTime(path.travelTime);

			leg.setRoute(wrappedRoute);
			travTime = path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null, wrappedNetwork);
			route.setTravelTime(0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		leg.setArrivalTime(depTime + travTime);
		return travTime;
	}
}
