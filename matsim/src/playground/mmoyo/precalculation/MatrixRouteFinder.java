package playground.mmoyo.precalculation;

import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.api.core.v01.network.Node;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;

public class MatrixRouteFinder {
	private AdjMatrix adjMatrix;
	private TransitSchedule transitSchedule;
	private NetworkLayer plainNet;
	List<List<NetworkRoute>> connectionList = new ArrayList<List<NetworkRoute>>();
	
	public MatrixRouteFinder(final TransitSchedule transitSchedule, final NetworkLayer plainNet) {
		this.transitSchedule= transitSchedule;
		this.plainNet = plainNet;
		adjMatrix = new AdjMatrix(this.transitSchedule, this.plainNet); 
	}

	public void findPTPath(final Coord coord1, final Coord coord2, double time, final double distToWalk){
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
		
		connectionList = new ArrayList<List<NetworkRoute>>();
		
		for (Node node1: nearOriginStops){
			for (Node node2: nearDestinationStops){
				//System.out.print("\nOrigen: "+ node1.getId() + "    Destination: " + node2.getId());
				findRoute(node1, node2);
			}
		}
		
		for (List<NetworkRoute> networkRouteList : connectionList){
			for (NetworkRoute networkRoute : networkRouteList ){
				System.out.println();
				for(Node node : networkRoute.getNodes()){
					System.out.print(node.getId() + " ");
				}
			}
		}

	}
	
	private Collection <NodeImpl> findNearStations(Coord coord, double walkRange){
		Collection <NodeImpl> stations;
		do{
			stations = plainNet.getNearestNodes(coord, walkRange);
			walkRange= walkRange + 300;
		} while (stations.size()<2);
		return stations;
	}
	
	public void findRoute(Node node1, Node node2){
		List <Id> adjRoutelist1 = adjMatrix.getAdjTransitRoutes(node1); 
		List <Id> adjRoutelist2 = adjMatrix.getAdjTransitRoutes(node2);
	
		System.out.println("\n origin routes");
		for (Id id :adjRoutelist1){
			System.out.println("  " + id);
		}
		
		System.out.println("destination routes");
		for (Id id : adjRoutelist2){
			System.out.println("  " + id);
		}
		
		List<NetworkRoute> networkRouteList = new ArrayList<NetworkRoute>();

		for(Id routeId1: adjRoutelist1){
			/**find connections without transfer*/
			TransitRoute transitRoute1= adjMatrix.getTransitRoute(routeId1);
			List<Node> nodeList1 = transitRoute1.getRoute().getNodes();
			if (containsBafterA(nodeList1, node1, node2)){
				NetworkRoute trip = transitRoute1.getRoute().getSubRoute(node1, node2);	
				networkRouteList.add(trip);
			}else{
				
				/**finds connections with 1 transfer*/
				for(Id routeId2: adjRoutelist2){
					TransitRoute transitRoute2= adjMatrix.getTransitRoute(routeId2);
					List<Node> nodeList2 = transitRoute2.getRoute().getNodes();
					int position1 =  nodeList1.indexOf(node1);
					for (int i= position1; i< nodeList1.size(); i++){
						Node node3= nodeList1.get(i);
						if (containsBafterA(nodeList2, node3, node2)){
							NetworkRoute trip1 = transitRoute1.getRoute().getSubRoute(node1, node3);	
							//NetworkRoute trip2 = transitRoute2.getRoute().getSubRoute(node3, node2);
							networkRouteList.add(trip1);
						}
					}
				}	
			}
		}
	}
	
	/**returns true if the nodeB is after nodeA in a transitRoute*/
	private boolean containsBafterA(List<Node> nodeList, Node nodeA, Node nodeB){
		boolean contains = false;
		if (nodeList.contains(nodeA) && nodeList.contains(nodeB) )
			contains= nodeList.indexOf(nodeB)> nodeList.indexOf(nodeA);
		return contains;
	}
}


/*
/**Creates a subroute from a given node until the end of the transitRoute
private List<Node> createSubRoute1(Node fromNode, TransitRoute transitRoute){
	NetworkRoute nodeTransitRoute1 = transitRoute.getRoute();
	int size = nodeTransitRoute1.getNodes().size();
	Node lastNode =  nodeTransitRoute1.getNodes().get(size-1);
	return  transitRoute.getRoute().getSubRoute(fromNode, lastNode).getNodes(); 
}

private List<Node> createSubRoute2(Node toNode, TransitRoute transitRoute){
	NetworkRoute nodeTransitRoute = transitRoute.getRoute();
	Node firstNode =  nodeTransitRoute.getNodes().get(0);
	boolean containsToNode = transitRoute.getRoute().getNodes().contains(toNode);
	List<Node> subRoute2 = null;
	if (containsToNode){
		subRoute2 = transitRoute.getRoute().getSubRoute(firstNode, toNode).getNodes(); 
	}
	return  subRoute2; 
}
*/