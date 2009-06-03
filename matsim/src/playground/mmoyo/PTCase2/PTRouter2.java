package playground.mmoyo.PTCase2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
//import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
//import org.matsim.core.api.population.Activity;
//import org.matsim.core.api.population.Leg;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
//import org.matsim.core.population.routes.LinkNetworkRoute;
//import org.matsim.core.router.Dijkstra;
import playground.mmoyo.PTRouter.PTDijkstra;

import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.mmoyo.PTRouter.PTNode;

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
public class PTRouter2 {
	private NetworkLayer net; 
	private PTDijkstra PTdijkstra;
	private PTDijkstra expressDijkstra;
	private PTTravelCost ptTravelCost;
	public PTTravelTime ptTravelTime;   //> make private 
	private PTTravelTime1 ptTravelTime1;
	//private int x=0;//--> Should be part of the method if the simulation strategy is set to re-route.
	
	public PTRouter2(NetworkLayer ptNetworkLayer, PTTimeTable2 ptTimetable) {
		this.net = ptNetworkLayer;
		this.ptTravelCost = new PTTravelCost(ptTimetable);
		this.ptTravelTime =new PTTravelTime(ptTimetable);
		this.PTdijkstra = new PTDijkstra(ptNetworkLayer, ptTravelCost, ptTravelTime);	
		this.expressDijkstra = new PTDijkstra(ptNetworkLayer, ptTravelCost, ptTravelTime1);
	}
	
	/**
	 * Main method to be invoked by other classes 
	 */
	public Path findRoute(Coord coord1, Coord coord2, double time, double distToWalk){
		//normal distance
		//Collection <Node> nearStops1 = net.getNearestNodes(coord1, distToWalk);
		//Collection <Node> nearStops2 = net.getNearestNodes(coord2, distToWalk);
		
		double distOridDest = CoordUtils.calcDistance(coord1,coord2);
		Collection <Node> nearStops1 = FindNearStops(coord1,  distToWalk);
		Collection <Node> nearStops2 = FindNearStops(coord2, distToWalk);
		
		Node ptNode1= CreateWalkingNode(new IdImpl("W1"), coord1);
		Node ptNode2=CreateWalkingNode(new IdImpl("W2"), coord2);

		List <Link> walkingLinkList1 = CreateWalkingLinks(ptNode1, nearStops1, true);
		List <Link> walkingLinkList2 = CreateWalkingLinks(ptNode2, nearStops2, false);

		Path path = PTdijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
		
		removeWalkingLinks(walkingLinkList1);
		removeWalkingLinks(walkingLinkList2);
		net.removeNode(ptNode1);
		net.removeNode(ptNode2);
		
		if (path!=null){
			path.nodes.remove(ptNode1);
			path.nodes.remove(ptNode2);
		}
		return path;
	}
	
	/**
	*if not nodes found in walk range then find the nearest one
	*/
	private Collection <Node> FindNearStops (final Coord coord, final double walkDistance){
		Collection <Node> NearStops = net.getNearestNodes(coord, walkDistance);
		if (NearStops.size()==0){
			Node nearNode = net.getNearestNode(coord);
			if (CoordUtils.calcDistance(coord, nearNode.getCoord())< walkDistance){
				NearStops.add(nearNode);
			}
		}
		return NearStops;
	}
	
	
	/**
	 *increments walk Range until a path is found
	 */
	public Path findPTPath(Coord coord1, Coord coord2, double time, final double distToWalk){
		double walkRange= distToWalk; 
		double OD_Distance = CoordUtils.calcDistance(coord1, coord2);
		
		Node origin= CreateWalkingNode(new IdImpl("W1"), coord1);
		Node destination=CreateWalkingNode(new IdImpl("W2"), coord2);
		
		List <Link> walkingLinkList1 = null;
		List <Link> walkingLinkList2= null;
		
		Collection <Node> nearStops1 =  net.getNearestNodes(coord1, walkRange);  
		Collection <Node> nearStops2 =  net.getNearestNodes(coord2, walkRange);	
		
		Path path= null;
		
		while (path==null && (walkRange<OD_Distance) && !nearStops1.contains(destination) && !nearStops2.contains(origin) ){
			//nearStops1 = net.getNearestNodes(coord1, walkRange);  
			//nearStops2 = net.getNearestNodes(coord2, walkRange);
			nearStops1 = FindnStations (origin, walkRange);
			nearStops2 = FindnStations (destination, walkRange);
			
			walkingLinkList1 = CreateWalkingLinks(origin, nearStops1, true);
			walkingLinkList2 = CreateWalkingLinks(destination, nearStops2, false);
			
			path = PTdijkstra.calcLeastCostPath(origin, destination, time); 
			
			removeWalkingLinks(walkingLinkList1);
			removeWalkingLinks(walkingLinkList2);
			
			walkRange= walkRange + 300;
			/*
			if (walkRange > 1) {
				System.out.println("coord1:" + coord1 + " coord2:" + coord2 + "distToWalk: " + walkRange + " OD_Distance: " + OD_Distance + " inRange: "+ nearStops1.size() );
			}
			*/
		}
	
		if (path!=null){
			path.nodes.remove(origin);
			path.nodes.remove(destination);
		}

		net.removeNode(origin);
		net.removeNode(destination);
		
		return path;
	}
	
	/**
	 * expands station search until a number of them is found
	 */
	private Collection <Node> FindnStations(Node node, double walkRange){
		Collection <Node> stations = net.getNearestNodes(node.getCoord(), walkRange);
		while (stations.size()<3){
			stations = net.getNearestNodes(node.getCoord(), walkRange);
			walkRange= walkRange+ 300;
			//System.out.println("Coord: " + coord + " walkRange: " + walkRange);
		}
		return stations;
	}
	
	public Node CreateWalkingNode(Id idNode, Coord coord) {
		//-> use node factory
		Node node = new PTNode(idNode, coord, "Walking");
		//ptNode.setIdPTLine(new IdImpl("Walk"));
		net.getNodes().put(idNode, node);
		return node;
	}
	
	public List <Link> CreateWalkingLinks(Node walkNode, Collection <Node> nearNodes, boolean to){
		//->move to link factory
		List<Link> NewWalkLinks = new ArrayList<Link>();
		String idLink;
		Node fromNode;
		Node toNode;
		int x=0;
		
		for (Node node : nearNodes){
			if (to){
				fromNode= walkNode;
				toNode= node;
				idLink= "WLO" + x++;
			}else{
				fromNode= node;
				toNode=  walkNode;
				idLink= "WLD" + x++;
			}
			Link link= createPTLink(idLink, fromNode, toNode, "Walking");
			//-->check if this temporary stuff improves the performance
			//link.setFreespeed(link.getLength()* WALKING_SPEED);
			NewWalkLinks.add(link);
		}
		return NewWalkLinks;
	}

	public Link createPTLink(String strIdLink, Node fromNode, Node toNode, String type){
		//->use link factory
		Id idLink = new IdImpl(strIdLink);
		double length = CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord());
		double freespeed= 1;
		double capacity = 1;
		double numLanes = 1;
		String origId = "0";
		return net.createLink(idLink, fromNode, toNode, length, freespeed, capacity, numLanes, origId, type);
	}
	
	public void removeWalkingLinks(Collection<Link> WalkingLinkList){
		//->use link factory
		for (Link link : WalkingLinkList){
			net.removeLink(link);
		}
	}
	
	public Path findRoute(Coord coord1, Coord coord2, double time){
		Node node1= net.getNearestNode(coord1);
		Node node2= net.getNearestNode(coord2);
		return findRoute(node1, node2,time);
	}
	
	public Path findRoute(Node ptNode1, Node ptNode2, double time){
		return PTdijkstra.calcLeastCostPath(ptNode1, ptNode2, time);
	}
	
	public void PrintRoute(Path path){
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
}//class


/*
public List<Object> findLegActs(Path path, double depTime){
	List<Object> actLegList = new ArrayList<Object>();
	if (path!=null){
		double legTravTime =0;
		double accumulatedTime=depTime;
		//double legArrTime=depTime;
		double routeTravelTime =0;
		int num=0;
	
		List<Link> linkList = new ArrayList<Link>();
		boolean first=true;
		List<Link> linkRoute = path.links;
		for(int x=0; x< linkRoute.size();x++){
			Link link = linkRoute.get(x);
			double linkTravelTime=ptTravelTime.getLinkTravelTime(link,accumulatedTime);
			accumulatedTime =accumulatedTime + linkTravelTime;
			routeTravelTime =routeTravelTime+linkTravelTime;
			
			//insert first ptActivity: boarding first PTVehicle
			if (first){ 
				Coord coord = link.getFromNode().getCoord();
				double startTime = 0; //this must be inmediately set when we know the passenger gets to the station
				double dur= 0;        //this must be inmediately set when we know the passenger gets to the station
				double endTime = depTime;
				actLegList.add(newPTAct(coord, link, startTime, dur, endTime));
				first=false;
			}
			
			if (link.getType().equals("Standard")){
				legTravTime = legTravTime+ linkTravelTime; 
				linkList.add(link);
			}else{
				//CarRoute legRoute = new NodeCarRoute();    25 feb
				LinkNetworkRoute legRoute = new LinkNetworkRoute(null, null); 
				
				legRoute.setTravelTime(routeTravelTime); //legRoute.setTravTime(routeTravelTime*3600);
				if (linkList.size()>0) {legRoute.setLinks(null, linkList, null);}
				
				//insert leg
				Leg leg = new org.matsim.core.population.LegImpl(TransportMode.pt);
				//routeTravelTime =routeTravelTime; // routeTravelTime =routeTravelTime*3600;  //Seconds
				leg.setDepartureTime(accumulatedTime);
				leg.setTravelTime(routeTravelTime);
				leg.setArrivalTime((accumulatedTime + (routeTravelTime)));
				//leg.setNum(num);   deprecated 		
				leg.setRoute(legRoute);
				actLegList.add(leg);		
				
				//clean variables
				linkList = new ArrayList<Link>();
				legTravTime=0;
				num++;
				
				//insert transfer activity  TODO: what about walking and other possible "pt modal choices"
				Coord coord = link.getToNode().getCoord();
				double startTime=depTime + routeTravelTime;
				double dur= linkTravelTime; //double dur= linkTravelTime*60;  //Seconds
				double endTime = startTime + dur;
				actLegList.add(newPTAct(coord,link, startTime, dur, endTime));
				
			}//if link = standard
			//set arrTime for the next loop:
			//legArrTime =  accumulatedTime;
			routeTravelTime=0;
				
		}// for x=0
	}//if route!null
	return actLegList;
}


private Activity newPTAct(Coord coord, Link link, double startTime, double dur, double endTime){
	Activity ptAct= new org.matsim.core.population.ActivityImpl("Wait PT Vehicle", coord);
	ptAct.setStartTime(startTime);
	ptAct.setEndTime(endTime);
	//ptAct.setDuration(dur);   deprecated
	//ptAct.calculateDuration();
	ptAct.setLink(link);
	//act.setDur(linkTravelTime*60);
	//act.setLinkId(link.getId());
	//act.setCoord(coord)
	return ptAct;
}
*/
