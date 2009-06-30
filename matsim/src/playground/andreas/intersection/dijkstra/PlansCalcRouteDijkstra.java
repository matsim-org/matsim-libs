package playground.andreas.intersection.dijkstra;

import java.util.ArrayList;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

/**
 * A AbstractPersonAlgorithm that calculates and sets the routes of a person's activities using {@link Dijkstra}.
 *
 * @author mrieser, aneumann
 */
public class PlansCalcRouteDijkstra extends PlansCalcRoute {
	
	NetworkLayer wrappedNetwork;
	Network originalNetwork;

	public PlansCalcRouteDijkstra(final Network originalNetwork, final NetworkLayer wrappedNetwork, final TravelCost costCalculator, final TravelTime timeCalculator) {
		this(originalNetwork, wrappedNetwork, costCalculator, timeCalculator, new FreespeedTravelTimeCost());
	}

	@SuppressWarnings("deprecation")
	private PlansCalcRouteDijkstra(final Network originalNetwork, final NetworkLayer wrappedNetwork, final TravelCost costCalculator, final TravelTime timeCalculator,
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
		Link fromLink = fromAct.getLink();
		Link toLink = toAct.getLink();
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = this.wrappedNetwork.getNode(fromLink.getId().toString());	// start at the end of the "current" link
		Node endNode = this.wrappedNetwork.getNode(toLink.getId().toString()); // the target is the start of the link

		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = this.getLeastCostPathCalculator().calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			
			ArrayList<Node> realRouteNodeList = new ArrayList<Node>();
			
			for (Node node : path.nodes) {
				realRouteNodeList.add(originalNetwork.getLinks().get(node.getId()).getToNode());
			}
			
			realRouteNodeList.remove(realRouteNodeList.size() - 1);
			
			NetworkRoute wrappedRoute = new NodeNetworkRoute();
			wrappedRoute.setNodes(realRouteNodeList);
			wrappedRoute.setTravelTime(path.travelTime);
			
			leg.setRoute(wrappedRoute);
			travTime = path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = new NodeNetworkRoute();
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
