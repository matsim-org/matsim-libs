package playground.mzilske.prognose2025;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.mzilske.pipeline.PersonSink;
import playground.mzilske.pipeline.PersonSinkSource;

public class PersonRouterFilter implements PersonSinkSource {
	
private Network network;
	
	private Dijkstra dijkstra;
	
	private PersonSink sink;
	
	private Collection<Id> interestingNodeIds = new HashSet<Id>();
	
	double travelTimeToEntry = 0.0;
	
	public PersonRouterFilter(Network network) {
		this.network = network;
		FreespeedTravelTimeCost fttc = new FreespeedTravelTimeCost(new CharyparNagelScoringConfigGroup());
		dijkstra = new Dijkstra(network, fttc, fttc);
	}

	private boolean isInteresting(Path path) {
		for (Node node : path.nodes) {
			if (interestingNodeIds.contains(node.getId())) {
				travelTimeToEntry = calculateFreespeedTravelTimeToNode(network, path, node);
				return true;
			}
		}
		return false;
	}

	@Override
	public void complete() {
		sink.complete();
	}

	Collection<Id> getInterestingNodeIds() {
		return interestingNodeIds;
	}

	@Override
	public void process(Person person) {
		ActivityImpl origin = (ActivityImpl) person.getPlans().get(0).getPlanElements().get(0);
		Activity destination = (Activity) person.getPlans().get(0).getPlanElements().get(2);
		Coord quelle = origin.getCoord();
		Node quellNode = ((NetworkImpl) network).getNearestNode(quelle);
		Coord ziel = destination.getCoord();
		Node zielNode = ((NetworkImpl) network).getNearestNode(ziel);
		Path path = dijkstra.calcLeastCostPath(quellNode, zielNode, 0.0);
		if (isInteresting(path)) {
			origin.setEndTime(origin.getEndTime() + travelTimeToEntry);
			sink.process(person);
		}
	}

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
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
