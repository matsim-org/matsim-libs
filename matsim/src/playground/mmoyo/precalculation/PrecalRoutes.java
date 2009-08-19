package playground.mmoyo.precalculation;

import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.Tuple;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;

import java.util.Map;
import java.util.TreeMap;
import org.matsim.core.basic.v01.IdImpl;

public class PrecalRoutes {
	private AdjList adjList;
	private TransitSchedule transitSchedule;
	private NetworkLayer plainNet;
	Map <Id, List<StaticConnection>> connectionMap = new TreeMap <Id, List<StaticConnection>>();
	
	public PrecalRoutes(final TransitSchedule transitSchedule, final NetworkLayer plainNet) {
		this.transitSchedule= transitSchedule;
		this.plainNet = plainNet;
		adjList = new AdjList(this.transitSchedule, this.plainNet); 
	}

	/**Returns the number of found connections and stores them in the connectionMap*/
	public int findPTPath(final Coord coord1, final Coord coord2, final double distToWalk){
		int connectionsNum =0;
		double walkRange= distToWalk; 
		Collection <NodeImpl> nearOriginStops = findNearStations (coord1, walkRange);
		Collection <NodeImpl> nearDestinationStops = findNearStations (coord2, walkRange);
		
		/*
		System.out.println("origin");
		for (Node node: nearOriginStops){
			System.out.print(node.getId() + " ");
		}
		
		System.out.println("\nDestination");
		for (Node node: nearDestinationStops){
			System.out.print(node.getId() + " ");
		}
		System.out.println(" ");
		*/

		
		for (Node node1: nearOriginStops){
			for (Node node2: nearDestinationStops){
				//System.out.print("\nOrigen: "+ node1.getId() + "    Destination: " + node2.getId());
				String strConnId = node1.getId().toString() + "-" + node2.getId().toString();
				Id connId= new IdImpl(strConnId);
				if (!connectionMap.containsKey(connId)){
					List<StaticConnection> connections =findRoute(node1, node2); 
					connectionMap.put(connId, connections);
					connectionsNum += connections.size();
				}
			}
		}
		
		return connectionsNum;

	}
	
	private Collection <NodeImpl> findNearStations(Coord coord, double walkRange){
		Collection <NodeImpl> stations;
		do{
			stations = plainNet.getNearestNodes(coord, walkRange);
			walkRange= walkRange + 300;
		} while (stations.size()<2);
		return stations;
	}
	
	public List<StaticConnection> findRoute(Node node1, Node node2){
		List<StaticConnection> connections = new ArrayList<StaticConnection>();
		List <TransitRoute> adjRoutelist1 = adjList.getAdjTransitRoutes(node1); 
		List <TransitRoute> adjRoutelist2 = adjList.getAdjTransitRoutes(node2);
		StaticConnection staticConnection = null;
		 
		/*
		System.out.println("\n origin routes");
		for (Id id :adjRoutelist1){
			System.out.println("  " + id);
		}
		
		System.out.println("destination routes");
		for (Id id : adjRoutelist2){
			System.out.println("  " + id);
		}
		*/
		
		for(TransitRoute transitRoute1: adjRoutelist1){
			
			/**find connections without transfer*/
			List<Node> nodeList1 = transitRoute1.getRoute().getNodes();
			if (containsBafterA(nodeList1, node1, node2)){
				PTtrip ptTrip = createTrip(transitRoute1, node1, node2);
				staticConnection = new StaticConnection();
				staticConnection.addPTtrip(ptTrip);
			}else{
				
				/**finds and stores connections with 1 transfer*/
				for(TransitRoute transitRoute2: adjRoutelist2){
					List<Node> nodeList2 = transitRoute2.getRoute().getNodes();
					int position1 =  nodeList1.indexOf(node1);
					for (int i= position1; i< nodeList1.size(); i++){
						Node node3= nodeList1.get(i);
						if (containsBafterA(nodeList2, node3, node2)){
							staticConnection = new StaticConnection();
							PTtrip ptTrip1 = createTrip(transitRoute1, node1, node3);
							PTtrip ptTrip2 = createTrip(transitRoute2, node3, node2);
							staticConnection.addPTtrip(ptTrip1);
							staticConnection.addPTtrip(ptTrip2);
						}
					}
				}	
			}//if contains
			connections.add(staticConnection);
		}//for id route
		return connections;
	}// find route
	
	/**returns true if nodeB is after nodeA in a transitRoute*/
	private boolean containsBafterA(List<Node> nodeList, Node nodeA, Node nodeB){
		boolean contains = false;
		if (nodeList.contains(nodeA) && nodeList.contains(nodeB) )
			contains= nodeList.indexOf(nodeB)> nodeList.indexOf(nodeA);
		return contains;
	}
	
	/**Experimental -must be integrated in function*/
	private List<StaticConnection> find3tripsConnections(Node nodeA, Node nodeB){
		List<StaticConnection> connections = new ArrayList<StaticConnection>();
		List <TransitRoute> adjRoutelistA = adjList.getAdjTransitRoutes(nodeA); 
		List <TransitRoute> adjRoutelistB = adjList.getAdjTransitRoutes(nodeB);
	
		for(TransitRoute transitRouteA: adjRoutelistA ){
			List<Node> nodeListA= transitRouteA.getRoute().getNodes();
			int position1 =  nodeListA.indexOf(nodeA);
			nodeListA = transitRouteA.getRoute().getNodes().subList(position1, nodeListA.size()-1);
			for (Node nodeI : nodeListA){
				//-> recursive for more transfers
				List <TransitRoute> adjRouteIList = adjList.getAdjTransitRoutes(nodeI);
				for (TransitRoute transitRouteI: adjRouteIList){
					List<Node> nodeListI= transitRouteI.getRoute().getNodes();
					for (TransitRoute transitRouteB: adjRoutelistB){
						Node nodeJ = findIntersNode(nodeListI,  transitRouteB);
						if (nodeJ!= null){
							List<Node> nodeListB= transitRouteB.getRoute().getNodes();
							if (containsBafterA(nodeListB, nodeB, nodeJ)){
								PTtrip ptTrip1 = createTrip(transitRouteA, nodeA, nodeI);
								PTtrip ptTrip2 = createTrip(transitRouteI, nodeI, nodeJ);
								PTtrip ptTrip3 = createTrip(transitRouteB, nodeJ, nodeB);
								StaticConnection staticConnection = new StaticConnection();
								staticConnection.addPTtrip(ptTrip1);
								staticConnection.addPTtrip(ptTrip2);
								staticConnection.addPTtrip(ptTrip3);
								connections.add(staticConnection);
							}
						}
					}
				}
			}
		}
		return connections;
	}
	
	/**returns the node where the given transitRoute intersects the list of nodes*/
	private Node findIntersNode(List<Node> nodeList, TransitRoute transitRoute){
		Node nullNode= null;
		for (Node node: nodeList){
			List<TransitRoute> adjRoutes = adjList.getAdjTransitRoutes(node);
			if (adjRoutes.contains(transitRoute))return node;
		}
		return nullNode;
	}
	
	
	/** returns the nodes where routeA and routeB intersect the given transitRoute */
	private Tuple<Node,Node> findAdjacentRoutes (TransitRoute transitRoute, TransitRoute routeA, TransitRoute routeB){
		Tuple<Node, Node> touple= null;
		int index=0;
		int indexA=-1;
		int indexB=-1;
		List<Node> netRoute= transitRoute.getRoute().getNodes();
		for (Node node: netRoute ){
			List<TransitRoute> adjRoutes = adjList.getAdjTransitRoutes(node); 
			if (adjRoutes.contains(routeA)) indexA= index; 
			if (adjRoutes.contains(routeB)) indexB= index;
			index++;
		}
		if (indexA>0 && indexB>0 && indexB>indexA){
			Node node1= netRoute.get(indexA);
			Node node2= netRoute.get(indexB);
			touple = new Tuple<Node, Node>(node1,node2);
		}
		return touple;
	}
	
	
	/**creates a PTtrip object*/
	private PTtrip createTrip(TransitRoute transitRoute, Node nodeA, Node nodeB){
		//calculate travelTime
		Id idA= nodeA.getId();
		Id idB= nodeB.getId();
		TransitStopFacility trStopFacilityA = transitSchedule.getFacilities().get(idA);
		TransitStopFacility trStopFacilityB = transitSchedule.getFacilities().get(idB);
		TransitRouteStop trStopA =transitRoute.getStop(trStopFacilityA);
		TransitRouteStop trStopB =transitRoute.getStop(trStopFacilityB);
		double travelTime = trStopB.getArrivalOffset() - trStopA.getDepartureOffset(); 

		Id trId = transitRoute.getId();
		NetworkRoute route = transitRoute.getRoute().getSubRoute(nodeA, nodeB);
		
		return new PTtrip(trId, route,travelTime);
	}
	
	
	
}

