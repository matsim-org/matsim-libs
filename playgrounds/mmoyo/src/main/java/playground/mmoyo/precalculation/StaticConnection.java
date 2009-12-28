package playground.mmoyo.precalculation;

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.network.Node;

/**Describes a connection as a simple sequence of trips without considering departure schedule*/ 
public class StaticConnection {
	private List<PTtrip> tripList = new ArrayList<PTtrip>();
	private double travelTime=0;
	private double distance=0;
	
	public StaticConnection() {
	}

	public void addPTtrip (final PTtrip ptTrip){
		this.tripList.add(ptTrip);
		this.distance +=  ptTrip.getRoute().getDistance();
		this.travelTime += ptTrip.getTravelTime();
		
		System.out.println("\nTrip Num:" + tripList.size());
		for (Node node: ptTrip.getRoute().getNodes()){
			System.out.print(node.getId() + " ");
		}
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
		return tripList.get(0).getRoute().getNodes().get(0);
	}
	
	public Node getToNode(){
		int lastTripSize =  tripList.get(tripList.size()-1).getRoute().getNodes().size();
		return  tripList.get(tripList.size()).getRoute().getNodes().get(lastTripSize-1);
	}
	
}