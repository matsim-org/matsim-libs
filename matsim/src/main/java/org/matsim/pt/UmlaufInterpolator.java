package org.matsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import java.util.List;

public class UmlaufInterpolator {

	private final Network network;
    private final LeastCostPathCalculator routingAlgo;

	public UmlaufInterpolator(Network network, final PlanCalcScoreConfigGroup config) {
		super();
		this.network = network;
        FreespeedTravelTimeAndDisutility travelTimes = new FreespeedTravelTimeAndDisutility(config);
		this.routingAlgo = new Dijkstra(network, travelTimes, travelTimes);
	}

	public void addUmlaufStueckToUmlauf(UmlaufStueck umlaufStueck, Umlauf umlauf) {
		List<UmlaufStueckI> umlaufStueckeOfThisUmlauf = umlauf.getUmlaufStuecke();
		if (! umlaufStueckeOfThisUmlauf.isEmpty()) {
			UmlaufStueckI previousUmlaufStueck = umlaufStueckeOfThisUmlauf.get(umlaufStueckeOfThisUmlauf.size() - 1);
			NetworkRoute previousCarRoute = previousUmlaufStueck.getCarRoute();
			Id<Link> fromLinkId = previousCarRoute.getEndLinkId();
			Id<Link> toLinkId = umlaufStueck.getCarRoute().getStartLinkId();
			if (!fromLinkId.equals(toLinkId)) {
				insertWenden(fromLinkId, toLinkId, umlauf);
			}
		}
		umlaufStueckeOfThisUmlauf.add(umlaufStueck);
	}

	private void insertWenden(Id<Link> fromLinkId, Id<Link> toLinkId, Umlauf umlauf) {
		Node startNode = this.network.getLinks().get(fromLinkId).getToNode();
		Node endNode = this.network.getLinks().get(toLinkId).getFromNode();
		double depTime = 0.0;
		Path wendenPath = routingAlgo.calcLeastCostPath(startNode, endNode, depTime, null, null);
		if (wendenPath == null) {
			throw new RuntimeException("No route found from node "
					+ startNode.getId() + " to node " + endNode.getId() + ".");
		}
		NetworkRoute route = new LinkNetworkRouteImpl(fromLinkId, toLinkId);
		route.setLinkIds(fromLinkId, NetworkUtils.getLinkIds(wendenPath.links), toLinkId);
		umlauf.getUmlaufStuecke().add(new Wenden(route));
	}

}
