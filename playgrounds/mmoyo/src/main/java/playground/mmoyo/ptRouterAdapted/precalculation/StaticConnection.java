package playground.mmoyo.ptRouterAdapted.precalculation;

import java.util.List;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.api.core.v01.Id;

/**Describes a connection as a simple sequence of legs without considering departure times*/
public class StaticConnection implements Comparable<StaticConnection>{
	private List<Leg> legs;
	private double travelTime=0;
	private double distance=0;
	private double walkDist = 0;
	private int ptTripNum=0;
	private String PT = "pt";
	private String TRWALK = "transit_walk";
	private Id id;
	
	public StaticConnection(Id id, List<Leg> legs, double travelTime, double distance, int transfers) {
		this.id = id;
		this.legs = legs;
		this.travelTime= travelTime;
		this.distance = distance;
		this.ptTripNum = transfers;
	}

	public Id getId(){
		return this.id;
	}
	
	public int getPtTripNum(){
		return this.ptTripNum;
	}

	public List<Leg> getLegs() {
		return this.legs;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public double getDistance() {
		return distance;
	}
	
	public double getFirstWalkTime(){
		return this.getLegs().get(0).getTravelTime();
	}
	
	public double getLastWalkTime(){
		return this.getLegs().get(this.getLegs().size()-1).getTravelTime();
	}

	public boolean isTheSameAs(StaticConnection other) {
		if (this.getFirstWalkTime() != other.getFirstWalkTime()){ 
			return false;
		}
		
		if (this.getLastWalkTime() != other.getLastWalkTime()){ 
			return false;
		}
		
		if (this.legs.size()!= other.legs.size()){
			return false;
		}

		for (int i=1; i< this.legs.size()-1; i++){
			Leg thisLeg = this.legs.get(i);
			Leg otherLeg = other.legs.get(i);
			if (thisLeg.getMode().equals(otherLeg.getMode())){
				if (thisLeg.getMode().equals(PT)){
					ExperimentalTransitRoute thisRoute = (ExperimentalTransitRoute) thisLeg.getRoute();
					ExperimentalTransitRoute otherRoute = (ExperimentalTransitRoute) otherLeg.getRoute();
					if (!thisRoute.getRouteDescription().equals(otherRoute.getRouteDescription())){
						return false;
					}
				}else if(thisLeg.getMode().equals(TRWALK)){
					if (thisLeg.getTravelTime()!= otherLeg.getTravelTime()){
						return false;
					}
				}
			}else{
				return false;
			}
		}

		return true;  //are the same
	}

	/**Compares two pt-connections to decide which one is "better", criteria: 1.- less transfers  2.-travel time*/
	@Override
	public int compareTo(StaticConnection o) {
		int diffTransfers = this.ptTripNum - o.getPtTripNum();  
		if (diffTransfers!=0){
			return diffTransfers;
		}
		//They have the same number of connections, now compare their travel time
		int doubleCompare = Double.compare(this.getTravelTime(), o.getTravelTime());
		if (doubleCompare!=0.0){
			return doubleCompare;
		}
			
		/*
		if (this.ptTripNum < o.getPtTripNum()){
			return -1; //this connection has fewer transfers than the other one
		} else if (this.ptTripNum < o.getPtTripNum()){
			return 1; //this connection has fewer transfers than the other one
		} else{   
			//They have the same number of connections, now compare their travel time
			if (this.getTravelTime() < o.getTravelTime()){
				 return -1;
			 }else if (this.getTravelTime() < o.getTravelTime()){
				 return 1;
			 }
		}
		*/
		
        //have same number of transfers and travel time, they are equal
		return 0;
	}
}
	