/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package herbie.running.pt;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;

import java.util.List;

public class DistanceCalculations {

	public static double getLegDistance(NetworkRoute route, Network network)
	{
		List<Id<Link>> ids = route.getLinkIds();
		
		double distance = 0.0;
		
		for (int i = 0; i < ids.size(); i++) {
			double dist = network.getLinks().get(ids.get(i)).getLength();
			
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
		
		double dist = route.getDistance();
		if(!Double.isNaN(dist)) return route.getDistance();
		
		String routeDescription = route.getRouteDescription();
		// events to legs generates a genericRoute with no description
		// for car legs from link i to link i (td, oct. 2012)
		String[] nodeIDs = routeDescription != null ?
			routeDescription.split(" ") :
			new String[0];
		
		for (int i = 0; i < (nodeIDs.length - 1); i++) {
			
			Node node1 = network.getNodes().get(Id.create(nodeIDs[i], Node.class));
			Node node2 = network.getNodes().get(Id.create(nodeIDs[i+1], Node.class));
			if (node1 == null) {
				System.out.println("could not find node with id " + nodeIDs[i]);
			}
			if (node2 == null) {
				System.out.println("could not find node with id " + nodeIDs[i+1]);
			}
			
			distance += (CoordUtils.calcEuclideanDistance(node1.getCoord(), node2.getCoord()));
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
		if(route instanceof NetworkRoute) {
			return getLegDistance((NetworkRoute) route, network);
		}
		else if(route instanceof ExperimentalTransitRoute){
			return getLegDistance((ExperimentalTransitRoute) route, network);
		}
		else {
			return getLegDistance((GenericRouteImpl) route, network);
		}
	}
	

	public static double getWalkDistance(Route route, Network network) {
		Coord fromCoord = network.getLinks().get(route.getStartLinkId()).getCoord();
		Coord toCoord = network.getLinks().get(route.getEndLinkId()).getCoord();
		return CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
	}

	public static double getLegDistance(ExperimentalTransitRoute route, Network network) {
		
		Coord startLinkCoord = network.getLinks().get(route.getStartLinkId()).getCoord();
		Coord endLinkCoord = network.getLinks().get(route.getEndLinkId()).getCoord();
		
		return CoordUtils.calcEuclideanDistance(startLinkCoord, endLinkCoord);
		
//		Double routeDist = route.getDistance();
//		if(!routeDist.isNaN()){
//			System.out.println();
//			return route.getDistance();
//		}
//		else if(route.getAccessStopId() == route.getEgressStopId() ||
//			route.getStartLinkId() == route.getEndLinkId()) {
//			return 0.0;
//		}
//		else{
//			Id linkIDStart = route.getStartLinkId();
//			Id linkIDEnd = route.getEndLinkId();
//			Coord startCoord = network.getLinks().get(linkIDStart).getCoord();
//			Coord endCoord = network.getLinks().get(linkIDEnd).getCoord();
//			return CoordUtils.calcDistance(startCoord, endCoord);
			// No solution found so far. See below for details.
//			return route.getDistance();
//		}
//		Here, the problem is that we dont have any departure time. But, so far, everything is running... bv, June 7, 2011
		
		
//		TransitRouterImpl t = (TransitRouterImpl) controler.getTransitRouterFactory();
//		Coord fromCoord = network.getLinks().get(route.getStartLinkId()).getCoord();
//		Coord toCoord = network.getLinks().get(route.getEndLinkId()).getCoord();
//		route.
//		List<Leg> legs = t.calcRoute(fromCoord, toCoord, departureTime);
		
//		Id linkIDStart = route.getStartLinkId();
//		Id linkIDEnd = route.getEndLinkId();
//		Coord startCoord = network.getLinks().get(linkIDStart).getCoord();
//		Coord endCoord = network.getLinks().get(linkIDEnd).getCoord();
//		return CoordUtils.calcDistance(startCoord, endCoord);
	}
}
