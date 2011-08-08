package org.matsim.pt;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

public class UmlaufInterpolator {

	private final Network network;
	private final PlanCalcScoreConfigGroup config;
	private final FreespeedTravelTimeCost travelTimes;
	private final LeastCostPathCalculator routingAlgo;

	public UmlaufInterpolator(Network network, final PlanCalcScoreConfigGroup config) {
		super();
		this.network = network;
		this.config = config;
		this.travelTimes = new FreespeedTravelTimeCost(this.config);
		this.routingAlgo = new Dijkstra(network, travelTimes, travelTimes);
	}

	public void addUmlaufStueckToUmlauf(UmlaufStueck umlaufStueck, Umlauf umlauf) {
		List<UmlaufStueckI> umlaufStueckeOfThisUmlauf = umlauf.getUmlaufStuecke();
		if (!umlaufStueckeOfThisUmlauf.isEmpty()) {
			UmlaufStueckI previousUmlaufStueck = umlaufStueckeOfThisUmlauf.get(umlaufStueckeOfThisUmlauf.size() - 1);
			NetworkRoute previousCarRoute = previousUmlaufStueck.getCarRoute();
			Id fromLinkId = previousCarRoute.getEndLinkId();
			Id toLinkId = umlaufStueck.getCarRoute().getStartLinkId();
			if (!fromLinkId.equals(toLinkId)) {
				insertWenden(fromLinkId, toLinkId, umlauf);
			}
		}
		umlaufStueckeOfThisUmlauf.add(umlaufStueck);
	}

	private void insertWenden(Id fromLinkId, Id toLinkId, Umlauf umlauf) {
		Node startNode = this.network.getLinks().get(fromLinkId).getToNode();
		Node endNode = this.network.getLinks().get(toLinkId).getFromNode();

		double depTime = 0.0;

		Path wendenPath = routingAlgo.calcLeastCostPath(startNode, endNode, depTime);
		if (wendenPath == null) {
			throw new RuntimeException("No route found from node "
					+ startNode.getId() + " to node " + endNode.getId() + ".");
		}
		NetworkRoute route = (NetworkRoute) ((NetworkFactoryImpl) this.network.getFactory())
				.createRoute(TransportMode.car, fromLinkId, toLinkId);
		route.setLinkIds(fromLinkId, NetworkUtils.getLinkIds(wendenPath.links), toLinkId);
		umlauf.getUmlaufStuecke().add(new Wenden(route));
	}

}
