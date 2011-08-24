package playground.tnicolai.urbansim.utils.helperObjects;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

public class AccessibilityHelperObject{
	
	private Map<Id,WorkplaceObject> numberOfWorkplacesPerZone;
	private double accessibilityTravelTimes;
	private double minTravelTime;
	
	/**
	 * constructor
	 */
	public AccessibilityHelperObject(Map<Id,WorkplaceObject> numberOfWorkplacesPerZone) {
		this.numberOfWorkplacesPerZone = numberOfWorkplacesPerZone;
		this.accessibilityTravelTimes = 0.;
		this.minTravelTime = Double.MAX_VALUE;
	}
	
	public void addNextAddend( double beta_per_min, 
			ActivityFacility destinationZone,
			double travelTime_min) {
		
		// get minimum travel time for in zone accessibility (see below)
		this.minTravelTime = Math.min(travelTime_min, this.minTravelTime);
		
		// this sum corresponds to the sum term of the log sum computation
		if (numberOfWorkplacesPerZone.get(destinationZone.getId()) != null) { // skipping zones with no workplaces
			long weight = numberOfWorkplacesPerZone.get(destinationZone.getId()).counter;
			
			double costFunction = Math.exp(beta_per_min * travelTime_min); // tnicolai: implement cost function as: Math.exp ( alpha * traveltime + beta * traveltime**2 + gamma * ln(traveltime) + delta * distance + epsilon * distance**2 + phi * ln(distance) ) 
			this.accessibilityTravelTimes += weight * costFunction;
		}
	}
	
	public void finalizeLogSum(double beta_per_min,
			ActivityFacility originZone){
		
		// add in zone accessibility (same zone computation as above but using (minTravelTime / 2) as travel cost cost )
		if(numberOfWorkplacesPerZone.get(originZone.getId()) != null){ // skipping zones no workplaces
			long weight = numberOfWorkplacesPerZone.get(originZone.getId()).counter;
			double costFunction = Math.exp( beta_per_min * (this.minTravelTime / 2) );
			this.accessibilityTravelTimes += weight * costFunction;
		}
	}
	
	public double getLogSum(){
		double logSum = Math.log( this.accessibilityTravelTimes );
		return logSum;
	}
}