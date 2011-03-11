package playground.mzilske.prognose2025;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class RouterFilter implements TripFlowSink {

	private Network network;

	private Dijkstra dijkstra;

	private TripFlowSink sink;

	private Collection<Id> interestingNodeIds = new HashSet<Id>();
	
	private double travelTimeToLink = 0.0;
	
	private Coord entryCoord;

	public RouterFilter(Network network) {
		this.network = network;
		FreespeedTravelTimeCost fttc = new FreespeedTravelTimeCost(new PlanCalcScoreConfigGroup());
		dijkstra = new Dijkstra(network, fttc, fttc);
	}

	@Override
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset) {
		Node quellNode = ((NetworkImpl) network).getNearestNode(quelle.coord);
		Node zielNode = ((NetworkImpl) network).getNearestNode(ziel.coord);
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0);
		if (isInteresting(path)) {
			Zone newQuelle = new Zone(quelle.id, quelle.workplaces, quelle.workingPopulation, entryCoord);
			sink.process(newQuelle, ziel, quantity, mode, destinationActivityType, departureTimeOffset + travelTimeToLink);
		}
	}

	private boolean isInteresting(Path path) {
		if (interestingNodeIds.contains(path.nodes.get(0))) {
			if (interestingNodeIds.contains(path.nodes.get(path.nodes.size()-1))) {
				return false;
			}
		}
		for (Node node : path.nodes) {
			if (interestingNodeIds.contains(node.getId())) {
				entryCoord = node.getCoord();
				travelTimeToLink = calculateFreespeedTravelTimeToNode(network, path, node);
				return true;
			}
		}
		return false;
	}

	void setSink(TripFlowSink sink) {
		this.sink = sink;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	Collection<Id> getInterestingNodeIds() {
		return interestingNodeIds;
	}

	private static double calculateFreespeedTravelTimeToNode(Network network, Path path, Node node) {
		double travelTime = 0.0;
		for (Link l : path.links) {
			if (l.getFromNode().equals(node)) {
				return travelTime;
			}
			travelTime += l.getLength() / l.getFreespeed();
			if (l.getToNode().equals(node)) {
				return travelTime;
			}
		}
		return travelTime;
	}

}
