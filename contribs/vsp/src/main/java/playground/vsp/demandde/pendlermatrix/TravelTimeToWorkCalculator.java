package playground.vsp.demandde.pendlermatrix;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.ActivityFacility;

public class TravelTimeToWorkCalculator implements TripFlowSink {

	private Network network;

	private LeastCostPathCalculator dijkstra;

	private TripFlowSink sink;

	public TravelTimeToWorkCalculator(Network network) {
		this.network = network;
		FreespeedTravelTimeAndDisutility fttc = new FreespeedTravelTimeAndDisutility(new PlanCalcScoreConfigGroup());
		dijkstra = new DijkstraFactory().createPathCalculator(network, fttc, fttc);
	}

	@Override
	public void process(ActivityFacility quelle, ActivityFacility ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset) {
		Node quellNode = NetworkUtils.getNearestNode(((Network) network),quelle.getCoord());
		Node zielNode = NetworkUtils.getNearestNode(((Network) network),ziel.getCoord());
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0, null, null);
		double travelTimeToWork = calculateFreespeedTravelTimeToNode(this.network, path, zielNode);
//		if(quelle.id == 9375 && ziel.id == 9162){
			System.out.println("from zone " + quelle.getId() + " to zone " + ziel.getId() + ", it takes " + travelTimeToWork + " seconds to travel.");
			sink.process(quelle, ziel, quantity, mode, destinationActivityType, departureTimeOffset - travelTimeToWork );
//		}
	}

	public void setSink(TripFlowSink sink) {
		this.sink = sink;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	private static double calculateFreespeedTravelTimeToNode(Network network, Path path, Node node) {
		double travelTime = 0.0;
		for (Link l : path.links) {
			travelTime += l.getLength() / l.getFreespeed();
		}
		return travelTime;
	}

}
