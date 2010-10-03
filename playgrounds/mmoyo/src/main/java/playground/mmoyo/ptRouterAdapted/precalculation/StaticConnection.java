package playground.mmoyo.ptRouterAdapted.precalculation;

import java.util.List;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.routes.ExperimentalTransitRoute;

/**Describes a connection as a simple sequence of legs without considering departure times*/
public class StaticConnection implements Comparable<StaticConnection>{
	private List<Leg> legs;
	private double travelTime=0;
	private double distance=0;
	private double walkDist = 0;
	private int ptTripNum=0;
	
	public StaticConnection(List<Leg> legs, double travelTime, double distance, int transfers) {
		this.legs = legs;
		this.travelTime= travelTime;
		this.distance = distance;
		this.ptTripNum = transfers;
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

	/* Already in constructor
	public int getPtLegNum(){
		int ptLegsNum = 0;
		String PT = "pt";
		for (Leg leg : this.getLegs()){
			if (leg.getMode().equals(PT)){
				ptLegsNum++;
			}
		}
		PT=null;
		return ptLegsNum;
	}
	*/
	
	
	@Override
	public int compareTo(StaticConnection other) {
		if (this.getFirstWalkTime() != other.getFirstWalkTime()){ 
			return -1;
		}
		
		if (this.getLastWalkTime() != other.getLastWalkTime()){ 
			return -1;
		}
		
		if (this.legs.size()!= other.legs.size()){
			return -1;
		}

		String PT = "pt";
		String TRWALK = "transit_walk";
		for (int i=1; i< this.legs.size()-1; i++){
			Leg thisLeg = this.legs.get(i);
			Leg otherLeg = other.legs.get(i);
			if (thisLeg.getMode().equals(otherLeg.getMode())){
				if (thisLeg.getMode().equals(PT)){
					ExperimentalTransitRoute thisRoute = (ExperimentalTransitRoute) thisLeg.getRoute();
					ExperimentalTransitRoute otherRoute = (ExperimentalTransitRoute) otherLeg.getRoute();
					if (!thisRoute.getRouteDescription().equals(otherRoute.getRouteDescription())){
						return -1;
					}
				}else if(thisLeg.getMode().equals(TRWALK)){
					if (thisLeg.getTravelTime()!= otherLeg.getTravelTime()){
						return-1;
					}
				}
			}else{
				return-1;
			}
		}

		return 0;  //are equal
	}
}
	