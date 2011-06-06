package herbie.running.pt;

import java.util.List;

import org.jgap.util.NetworkKit;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class DistanceCalculations {

	public static double getLegDistance(LinkNetworkRouteImpl route, Network network)
	{
		List<Id> ids = route.getLinkIds();
		
		double distance = 0.0;
		
		for (int i = 0; i < ids.size(); i++) {
			Double dist = network.getLinks().get(ids.get(i)).getLength();
			
			distance += network.getLinks().get(ids.get(i)).getLength();
		}
		
		Link startLink = network.getLinks().get(route.getStartLinkId());
		Link endLink = network.getLinks().get(route.getEndLinkId());
		distance += (startLink.getLength() / 2.0);
		distance += (endLink.getLength() / 2.0);
		
		return distance;
	}
	
	public static double getLegDistance(GenericRouteImpl route, Network network){
		
		double distance = 0.0;
		
		Double dist = route.getDistance();
		if(!dist.isNaN()) return route.getDistance();
		
		String routeDescription = route.getRouteDescription();
		String nodeIDs[] = routeDescription.split(" ");
		
		for (int i = 0; i < (nodeIDs.length - 1); i++) {
			
			Node node1 = network.getNodes().get(new IdImpl(nodeIDs[i]));
			Node node2 = network.getNodes().get(new IdImpl(nodeIDs[i+1]));
			
			distance += (CoordUtils.calcDistance(node1.getCoord(), node2.getCoord()));
		}
		
		Link startLink = network.getLinks().get(route.getStartLinkId());
		Link endLink = network.getLinks().get(route.getEndLinkId());
		distance += (startLink.getLength() / 2.0);
		distance += (endLink.getLength() / 2.0);
		
		return distance;
	}
	

	/**
	 * Returns the leg distance of either Link NetworkRouteImpl or a GenericRouteImpl
	 * The lengths of the start and end links are cut in halves.
	 * @param route
	 * @param network
	 * @return distance in m !!
	 */
	
	public static double getLegDistance(Route route, Network network) 
	{
		if(route instanceof LinkNetworkRouteImpl) {
			return getLegDistance((LinkNetworkRouteImpl) route, network);
		}
		else {
			return getLegDistance((GenericRouteImpl) route, network);
		}
	}
	

	public static double getTransitWalkDistance(GenericRouteImpl route, Network network) {
		Coord fromCoord = network.getLinks().get(route.getStartLinkId()).getCoord();
		Coord toCoord = network.getLinks().get(route.getEndLinkId()).getCoord();
		return CoordUtils.calcDistance(fromCoord, toCoord);
	}
	
}