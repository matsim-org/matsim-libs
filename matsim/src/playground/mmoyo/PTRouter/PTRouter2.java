package playground.mmoyo.PTRouter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.MyDijkstra;
import playground.mmoyo.PTRouter.PTNode;
import playground.mmoyo.TransitSimulation.LogicIntoPlainTranslator;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
/** 
 * Second version of Router using Matsims Class Dijkstra  
 * We avoid the relationship with the city network and use coordinate search instead
 *  *
 * @param nodeList PTNodes in stored a a Node-List
 * @param linkList Collection of org.matsim.network.Link
 * @param OriginNode Node where the trip begins
 * @param DestinationNode Node where the trip must finish
 * @param ptLinkCostCalculator Class that contains the weight information of links
 * @param time Milliseconds after the midnight in which the trip must begin
 */
public class PTRouter2{
	private NetworkLayer logicNet;
	private LeastCostPathCalculator myDijkstra;
	private TravelCost ptTravelCost;
	public TravelTime ptTravelTime;   //> make private 
	
	public PTRouter2(NetworkLayer logicNet, PTTimeTable2 ptTimetable) {
		this.logicNet = logicNet;
		this.ptTravelCost = new PTTravelCost(ptTimetable);
		this.ptTravelTime =new PTTravelTime(ptTimetable);
		this.myDijkstra = new MyDijkstra(logicNet, ptTravelCost, ptTravelTime);	
	}
	
	public PTRouter2(NetworkLayer logicNet, PTTimeTable2 ptTimetable, LogicIntoPlainTranslator logicToPlainConverter) {
		this.logicNet = logicNet;
		this.ptTravelCost = new PTTravelCost(ptTimetable);
		this.ptTravelTime =new PTTravelTime(ptTimetable);
		this.myDijkstra = new MyDijkstra(logicNet, ptTravelCost, ptTravelTime);		
	}
	
	public Path findPTPath(Coord coord1, Coord coord2, double time, final double distToWalk){
		double walkRange= distToWalk; 
		Node origin= createWalkingNode(new IdImpl("W1"), coord1);
		Node destination= createWalkingNode(new IdImpl("W2"), coord2);
		
		Collection <Node> nearOriginStops = findnStations (coord1, walkRange);
		Collection <Node> nearDestinationStops = findnStations (coord2, walkRange);
		
		List <Link> walkLinksFromOrigin = createWalkingLinks(origin, nearOriginStops, true);
		List <Link> walkLinksToDestination = createWalkingLinks(destination, nearDestinationStops, false);
			
		Path path = myDijkstra.calcLeastCostPath(origin, destination, time); 
			
		removeWalkLinks(walkLinksFromOrigin);
		removeWalkLinks(walkLinksToDestination);
		if (path!=null){
			path.nodes.remove(origin);
			path.nodes.remove(destination);
		}
		logicNet.removeNode(origin);
		logicNet.removeNode(destination);
		
		return path;
	}

	private Collection <Node> findnStations(Coord coord, double walkRange){
		Collection <Node> stations;
		do{
			stations = logicNet.getNearestNodes(coord, walkRange);
			walkRange= walkRange + 300;
		} while (stations.size()<2);
		return stations;
	}

	/**
	 * Creates a temporary origin or destination node
	 * avoids the method net.createNode because it is not necessary to rebuild the quadtree
	 */
	public Node createWalkingNode(Id id, Coord coord) {
		Node node = new PTNode(id, coord, "Walking");
		logicNet.getNodes().put(id, node);
		return node;
	}
	
	public List <Link> createWalkingLinks(Node walkNode, Collection <Node> nearNodes, boolean to){
		//->move to link factory
		List<Link> newWalkLinks = new ArrayList<Link>();
		Id idLink;
		Node fromNode;
		Node toNode;
		int x=0;
		
		
		for (Node node : nearNodes){
			if (to){
				fromNode= walkNode;
				toNode= node;
				idLink = new IdImpl("WLO" + x++);
			}else{
				fromNode= node;
				toNode=  walkNode;
				idLink = new IdImpl("WLD" + x++);
			}
			Link link= logicNet.createLink(idLink, fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()) , 1, 1, 1, "0", "Walking");
			//-->check if this temporary stuff improves the performance
			//link.setFreespeed(link.getLength()* WALKING_SPEED);
			newWalkLinks.add(link);
		}
		return newWalkLinks;
	}

	public void removeWalkLinks(Collection<Link> WalkingLinkList){
		//->use link factory
		for (Link link : WalkingLinkList){
			logicNet.removeLink(link);
		}
	}
	
	public Path findRoute(Coord coord1, Coord coord2, double time){
		Node node1= logicNet.getNearestNode(coord1);
		Node node2= logicNet.getNearestNode(coord2);
		return findRoute(node1, node2,time);
	}
	
	public Path findRoute(Node ptNode1, Node ptNode2, double time){
		return myDijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
	}
	
	public void printRoute(Path path){
		if (path!=null){
			System.out.print("\nLinks: ");
			//for (Link l L route.getLinks()) {
				//System.out.println("link: "l.getId() + " cost: " + link.);
			//}
		
			Id idPTLine = new IdImpl("");
			for (Node node : path.nodes){
				PTNode ptNode= (PTNode)node;
				if(ptNode.getIdPTLine()==idPTLine){
					System.out.print(ptNode.getId().toString() + " ");
				}else{
					System.out.println("\n" + ptNode.getIdPTLine().toString());
					System.out.print(ptNode.getId().toString() + " ");
				}
				idPTLine= ptNode.getIdPTLine();	
			}
			System.out.println("\nTravel cost of route=" + path.travelCost + "  time of route:" + path.travelTime);
		}else{
			System.out.println("The route is null");
		}
	}	
}



/*
 * searches nodes in walk range or else the nearest one
*/
/*
private Collection <Node> findNearStops (final Coord coord, final double walkDistance){
	Collection <Node> NearStops = net.getNearestNodes(coord, walkDistance);
	if (NearStops.size()==0){
		Node nearNode = net.getNearestNode(coord);
		if (CoordUtils.calcDistance(coord, nearNode.getCoord())< walkDistance){
			NearStops.add(nearNode);
		}
	}
	return NearStops;
}
*/


