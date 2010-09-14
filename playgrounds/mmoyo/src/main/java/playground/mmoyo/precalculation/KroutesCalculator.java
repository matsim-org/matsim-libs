package playground.mmoyo.precalculation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

public class KroutesCalculator {
	private AdjList adjList;
	private TransitSchedule transitSchedule;
	private NetworkImpl plainNet;
	private Map <Id, List<StaticConnection>> connectionMap;
	private Map <Coord, Collection<NodeImpl>> nearStopMap;

	/**Calculates a set of connections between two coordinates*/
	public KroutesCalculator(final TransitSchedule transitSchedule, final NetworkImpl plainNet, Map <Id, List<StaticConnection>> connectionMap, Map <Coord, Collection<NodeImpl>> nearStopMap) {
		this.transitSchedule= transitSchedule;
		this.plainNet = plainNet;
		this.connectionMap = connectionMap;
		this.nearStopMap = nearStopMap;
		adjList = new AdjList(this.transitSchedule, this.plainNet);
	}

	/****finds connections between two nodes and stores them in the connectionMap*/
	public int findPTPath(final Coord coord1, final Coord coord2, final double distToWalk){
		int connectionsNum =0;
		double walkRange= distToWalk;
		Collection <Node> nearOriginStops = findNearStations (coord1, walkRange);
		Collection <Node> nearDestinationStops = findNearStations (coord2, walkRange);

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

	private Collection <Node> findNearStations(final Coord coord, double walkRange){
		Collection <Node> stations;
		do{
			stations = plainNet.getNearestNodes(coord, walkRange);
			walkRange= walkRange + 300;
		} while (stations.size()<2);
		/* Coord is not comparable =(
		if (!nearStopMap.containsKey(coord)){
			nearStopMap.put(coord, stations);
		}*/
		return stations;
	}

	/**finds connections between two nodes with 0 or 1 transfer*/
	public List<StaticConnection> findRoute(final Node node1, final Node node2){
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

			//find connections without transfer*/
			List<Node> nodeList1 = RouteUtils.getNodes(transitRoute1.getRoute(), this.plainNet);
			if (containsBafterA(nodeList1, node1, node2)){
				PTtrip ptTrip = createTrip(transitRoute1, node1, node2);
				staticConnection = new StaticConnection(this.plainNet);
				staticConnection.addPTtrip(ptTrip);
			}else{

				//*finds and stores connections with 1 transfer*/
				for(TransitRoute transitRoute2: adjRoutelist2){
					List<Node> nodeList2 = RouteUtils.getNodes(transitRoute2.getRoute(), this.plainNet);
					int position1 =  nodeList1.indexOf(node1);
					for (int i= position1; i< nodeList1.size(); i++){
						Node node3= nodeList1.get(i);
						if (containsBafterA(nodeList2, node3, node2)){
							staticConnection = new StaticConnection(this.plainNet);
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
	private boolean containsBafterA(final List<Node> nodeList, final Node nodeA, final Node nodeB){
		boolean contains = false;
		if (nodeList.contains(nodeA) && nodeList.contains(nodeB) )
			contains= nodeList.indexOf(nodeB)> nodeList.indexOf(nodeA);
		return contains;
	}

	/**Experimental -must be integrated in function*/
	private List<StaticConnection> find3tripsConnections(final Node nodeA, final Node nodeB){
		List<StaticConnection> connections = new ArrayList<StaticConnection>();
		List <TransitRoute> adjRoutelistA = adjList.getAdjTransitRoutes(nodeA);
		List <TransitRoute> adjRoutelistB = adjList.getAdjTransitRoutes(nodeB);

		for(TransitRoute transitRouteA: adjRoutelistA ){
			List<Node> nodeListA= RouteUtils.getNodes(transitRouteA.getRoute(), this.plainNet);
			int position1 =  nodeListA.indexOf(nodeA);
			nodeListA = nodeListA.subList(position1, nodeListA.size()-1);
			for (Node nodeI : nodeListA){
				//-> recursive for more transfers
				List <TransitRoute> adjRouteIList = adjList.getAdjTransitRoutes(nodeI);
				for (TransitRoute transitRouteI: adjRouteIList){
					List<Node> nodeListI= RouteUtils.getNodes(transitRouteI.getRoute(), this.plainNet);
					for (TransitRoute transitRouteB: adjRoutelistB){
						Node nodeJ = findIntersNode(nodeListI,  transitRouteB);
						if (nodeJ!= null){
							List<Node> nodeListB = RouteUtils.getNodes(transitRouteB.getRoute(), this.plainNet);
							if (containsBafterA(nodeListB, nodeB, nodeJ)){
								PTtrip ptTrip1 = createTrip(transitRouteA, nodeA, nodeI);
								PTtrip ptTrip2 = createTrip(transitRouteI, nodeI, nodeJ);
								PTtrip ptTrip3 = createTrip(transitRouteB, nodeJ, nodeB);
								StaticConnection staticConnection = new StaticConnection(this.plainNet);
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
	private Node findIntersNode(final List<Node> nodeList, final TransitRoute transitRoute){
		Node nullNode= null;
		for (Node node: nodeList){
			List<TransitRoute> adjRoutes = adjList.getAdjTransitRoutes(node);
			if (adjRoutes.contains(transitRoute))return node;
		}
		return nullNode;
	}


	/** returns the nodes where routeA and routeB intersect the given transitRoute */
	private Tuple<Node,Node> findAdjacentRoutes (final TransitRoute transitRoute, final TransitRoute routeA, final TransitRoute routeB){
		Tuple<Node, Node> touple= null;
		int index=0;
		int indexA=-1;
		int indexB=-1;
		List<Node> netRoute= RouteUtils.getNodes(transitRoute.getRoute(), this.plainNet);
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
	private PTtrip createTrip(final TransitRoute transitRoute, final Node nodeA, final Node nodeB){
		/* calculates travelTime */
		Id idA= nodeA.getId();
		Id idB= nodeB.getId();
		TransitStopFacility trStopFacilityA = transitSchedule.getFacilities().get(idA);
		TransitStopFacility trStopFacilityB = transitSchedule.getFacilities().get(idB);
		TransitRouteStop trStopA =transitRoute.getStop(trStopFacilityA);
		TransitRouteStop trStopB =transitRoute.getStop(trStopFacilityB);
		double travelTime = trStopB.getArrivalOffset() - trStopA.getDepartureOffset();
		NetworkRoute subRoute = RouteUtils.getSubRoute(transitRoute.getRoute(), nodeA, nodeB, this.plainNet);
		return new PTtrip(transitRoute, subRoute, travelTime, this.plainNet);
	}
}

