package playground.mmoyo.precalculation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;

/**Selects the best connections*/  
public class ConnectionRate {
	private Map <Coord, Collection<NodeImpl>> nearStopMap; 
	private Map <Id, List<StaticConnection>> connectionMap;
	
	public ConnectionRate (Map <Id, List<StaticConnection>> connectionMap, Map <Coord, Collection<NodeImpl>> nearStopMap){
		this.nearStopMap = nearStopMap;
		this.connectionMap = connectionMap;
	}

	public List<StaticConnection> selectBestConnections(int number, List<StaticConnection> staticConnectionList){
		List<StaticConnection> bestConnectionList= null;
		
		int size = staticConnectionList.size();
		double parameters[][] = new double[3][size];
		
		for (int i= 0; i<size; i++){
			StaticConnection staticConnection =  bestConnectionList.get(i); 
			parameters[i][0] = staticConnection.getTransferNum(); 
			parameters[i][1] = staticConnection.getTravelTime(); 
			parameters[i][2] = staticConnection.getDistance();
			//parameters[i][3] = staticConnection.  monetary cost?
			//parameters[i][4] = staticConnection.  walk distances?
		}
		//  Apply sort algorithm for this matrix 
		return bestConnectionList;
	}
	
	public StaticConnection getBestConnection(ActivityImpl act1, ActivityImpl act2) {

		StaticConnection bestConnection=null;
		Collection<NodeImpl> nearStops1 = nearStopMap.get(act1.getCoord());		
		Collection<NodeImpl> nearStops2 = nearStopMap.get(act2.getCoord());
		
		for (NodeImpl node1: nearStops1) {
			for (NodeImpl node2: nearStops2) {
				String strConnId = node1.getId().toString() + "-" + node2.getId().toString();
				List<StaticConnection> connections = connectionMap.get(strConnId);
				
				//->Apply criteria to select the best connection. Temporarily the shortest one*/
				double minDistance= Double.POSITIVE_INFINITY;
		
				for (StaticConnection staticConnection : connections) {
					if (staticConnection.getDistance()< minDistance){
						minDistance = staticConnection.getDistance();
						bestConnection = staticConnection ;
					}
				}
			}
		}
		return bestConnection;
	}

}
