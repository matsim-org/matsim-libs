package playground.mmoyo.precalculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

/**
 * Creates a representation of a special adjacency List [StopFacilities X TransitRoutes]
 * @author manuel
 */
public class AdjList {
	private TransitSchedule transitSchedule;
	private Map <Id, List<TransitRoute>> AdjMap = new TreeMap <Id, List<TransitRoute>>();   //idStopFacility, <Routes>
	private NetworkImpl plainNet;

	public AdjList(final TransitSchedule transitSchedule, final NetworkImpl plainNet ) {
		this.plainNet = plainNet;
		this.transitSchedule= transitSchedule;
		createAdjList();
	}

	private void createAdjList(){

		//-->validate the relation of Stopfacilities and transitRoute.route.Stopfacilities

		/**fill up adjList with StopFacilities Ids and empty route lists*/
		for (Id idFacility : transitSchedule.getFacilities().keySet()){
			AdjMap.put(idFacility, new ArrayList<TransitRoute>());
		}

		/**Read all transitRoutes and store its nodes id's in their corresponding id facility*/
		for (TransitLine transitLine : transitSchedule.getTransitLines().values()){
			for (TransitRoute transitRoute : transitLine.getRoutes().values()){
				List<Node> nodeList = new ArrayList<Node>();
				for (TransitRouteStop transitRouteStop: transitRoute.getStops()){
					AdjMap.get(transitRouteStop.getStopFacility().getId()).add(transitRoute);
					nodeList.add(plainNet.getNodes().get(transitRouteStop.getStopFacility().getId()));
				}

		 		/**sets also route as nodes.*/
				NetworkRoute nodeRoute = new LinkNetworkRouteImpl(null, null);
				nodeRoute.setLinkIds(null, NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(nodeList)), null);
				transitRoute.setRoute(nodeRoute);
			}
		}
	}

	/**returns the transitRoutes that travel through a given node. Node = stopFacility */
	public List<TransitRoute> getAdjTransitRoutes (Node node){
		return AdjMap.get(node.getId());
	}

	/**prints all stopFacilities and their correspondent adjacent TransitRoutes*/
	public void printMap(){
		for(Map.Entry <Id,List<TransitRoute>> entry: AdjMap.entrySet() ){
			System.out.print(entry.getKey() + ": [" );
			for (TransitRoute transitRoute : entry.getValue()){
				System.out.print(transitRoute.getId()+ ", " );
			}
			System.out.println("]");
		}
	}
}
