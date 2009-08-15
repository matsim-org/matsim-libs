package playground.mmoyo.precalculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NodeNetworkRoute;

/**
 * Creates a representation of a special adjacency matrix [StopFacilities X TransitRoutes]
 * @author manuel
 */

public class AdjMatrix {
	private TransitSchedule transitSchedule;
	private Map <Id, List<Id>> AdjMap = new TreeMap <Id, List<Id>>();   //idStopFacility, <IdRoutes>
	private NetworkLayer plainNet;
	private Map <Id, TransitRoute> transitRouteMap = new TreeMap <Id, TransitRoute>();   
	
	/**constructor*/
	public AdjMatrix(final TransitSchedule transitSchedule, NetworkLayer plainNet ) {
		this.plainNet = plainNet;
		this.transitSchedule= transitSchedule;
		createAdjMatrix();
	}
	
	private void createAdjMatrix(){
		
		//-->validate the relation of Stopfacilities and transitRoute.route.Stopfacilities
		
		/**fill up matrix with StopFacilities Ids and empty route lists*/
		for (Id idFacility : transitSchedule.getFacilities().keySet()){
			List<Id> TransitRouteIdList = new ArrayList<Id>();
			AdjMap.put(idFacility, TransitRouteIdList);
		}

		/**Read all transitRoutes and store its nodes id's in their corresponding id facility*/
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				List<Node> nodeList = new ArrayList<Node>();
				Id routeId = transitRoute.getId();

				for (TransitRouteStop transitRouteStop: transitRoute.getStops()){
					Id stopId = transitRouteStop.getStopFacility().getId();
					AdjMap.get(stopId).add(routeId);
					
					Node node = plainNet.getNode(stopId);
					nodeList.add(node);
				}
				
		 		/**sets also route as nodes.*/ 
				NetworkRoute nodeRoute = new NodeNetworkRoute(null, null);
				nodeRoute.setNodes(null, nodeList, null);
				transitRoute.setRoute(nodeRoute);
				transitRouteMap.put(routeId, transitRoute);
			}
		}
	}

	/**returns the transitRoutes that travel through a given node. Node = stopFacility */
	public List<Id> getAdjTransitRoutes (Node node){
		return AdjMap.get(node.getId());	
	}
	
	public TransitRoute getTransitRoute (Id id){
		return transitRouteMap.get(id);
	}
	
	/**prints all stopFacilities and their adjacent TransitRoutes*/
	public void printMap(){
		for(Map.Entry <Id,List<Id>> entry: AdjMap.entrySet() ){
			Id key = entry.getKey(); 
			List<Id> value = entry.getValue();
			System.out.print(key+ ": [" );
			for (Id id : value){
				System.out.print(id + ", " );
			}
			System.out.println("]");
		}
	}
}
