package playground.toronto.sotr;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.pt.router.TransitRouter;

import playground.toronto.sotr.SOTRMultiNodeDijkstra.Path;
import playground.toronto.sotr.calculators.SOTRDisutilityCalculator;
import playground.toronto.sotr.calculators.SOTRTimeCalculator;
import playground.toronto.sotr.routernetwork2.AbstractRoutingLink;
import playground.toronto.sotr.routernetwork2.RoutingNetwork.RoutingNetworkDelegate;
import playground.toronto.sotr.routernetwork2.AbstractRoutingNode;

public class SecondOrderTransitRouter implements TransitRouter {

	private final RoutingNetworkDelegate network;
	private final SOTRMultiNodeDijkstra pathingAlgorithm;
	//private final SOTRTimeCalculator timeCalc;
	//private final SOTRDisutilityCalculator costCalc;
	
	private final double searchRadius;
	private final double extensionRadius;
	
	public SecondOrderTransitRouter(final RoutingNetworkDelegate network, final SOTRTimeCalculator timeCalc,
			final SOTRDisutilityCalculator costCalc, final double searchRadius, final double extensionRadius) {
		this.network = network;
		//this.timeCalc = timeCalc;
		//this.costCalc = costCalc;
		
		this.searchRadius = searchRadius;
		this.extensionRadius = extensionRadius;
		
		this.pathingAlgorithm = new SOTRMultiNodeDijkstra(timeCalc, costCalc, network);
	}
	
	@Override
	public List<Leg> calcRoute(Coord fromCoord, Coord toCoord,
			double departureTime, Person person) {
		
		//Prepare the network delegate for routing.
		Collection<AbstractRoutingNode> accessNodes = getNearestNodes(fromCoord);
		Collection<AbstractRoutingNode> egressNodes = getNearestNodes(toCoord);
		this.network.prepareForRouting(fromCoord, accessNodes, toCoord, egressNodes);
		
		//Execute the routing algorithm
		Path path = this.pathingAlgorithm.calculateLeastCostPath(person, departureTime);
		if (path == null) return null;
		if (path.size() == 0) return null;
		
		//Convert the path to a list of legs
		for (AbstractRoutingLink link : path.getMiddleLinks()){
			
			//link.getFromNode().getStopFacility(null, null);
			
			
			//For in-vehicle episodes
			//ExperimentalTransitRoute(final TransitStopFacility accessFacility, final TransitLine line, final TransitRoute route, final TransitStopFacility egressFacility)

		}
		
		return null;
	}
	
	private Collection<AbstractRoutingNode> getNearestNodes(Coord coord){
		Collection<AbstractRoutingNode> nodes = network.getNearestNodes(coord, searchRadius);
		if (nodes.size() < 2){
			nodes = network.getNearestNodes(coord, searchRadius + extensionRadius);
		}
		
		return nodes;
	}
	
	

}
