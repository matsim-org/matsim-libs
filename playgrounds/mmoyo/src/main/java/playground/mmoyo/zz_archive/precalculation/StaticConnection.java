package playground.mmoyo.zz_archive.precalculation;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.RouteUtils;

/**Describes a connection as a simple sequence of trips without considering departure schedule*/
public class StaticConnection {
	private List<PTtrip> tripList = new ArrayList<PTtrip>();
	private double travelTime=0;
	private double distance=0;
	private Network network;

	public StaticConnection(Network network) {
		this.network = network;
	}

	public void addPTtrip (final PTtrip ptTrip){
		this.tripList.add(ptTrip);
		this.distance +=  ptTrip.getRoute().getDistance();
		this.travelTime += ptTrip.getTravelTime();

		System.out.println("\nTrip Num:" + tripList.size());
//		for (Node node: ptTrip.getRoute().getNodes()){
//			System.out.print(node.getId() + " ");
//		}
		System.out.println ("\n connection distance :" + this.distance);
		System.out.println (" connection travel time:" + this.travelTime);

	}

	public int getTransferNum(){
		return this.tripList.size()-1;
	}

	public List<PTtrip> getTripList() {
		return tripList;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public double getDistance() {
		return distance;
	}

	public Node getFromNode(){
		return RouteUtils.getNodes(tripList.get(0).getRoute(), this.network).get(0);
	}

	public Node getToNode(){
		int lastTripSize =  tripList.get(tripList.size()-1).getRoute().getLinkIds().size() + 1;
		return  RouteUtils.getNodes(tripList.get(tripList.size()).getRoute(), this.network).get(lastTripSize-1);
	}

}