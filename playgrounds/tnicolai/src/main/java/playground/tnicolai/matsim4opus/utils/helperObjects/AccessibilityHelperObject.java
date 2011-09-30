package playground.tnicolai.matsim4opus.utils.helperObjects;

import java.util.Map;

import org.matsim.api.core.v01.Id;

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
			Id destinationZoneID,
			double travelTime_min) {
		
		// get minimum travel time for in zone accessibility (see below)
		this.minTravelTime = Math.min(travelTime_min, this.minTravelTime);
		
		// this sum corresponds to the sum term of the log sum computation
		if (numberOfWorkplacesPerZone.get( destinationZoneID ) != null) { // skipping zones with no workplaces
			long weight = numberOfWorkplacesPerZone.get( destinationZoneID ).counter;
			
			double costFunction = Math.exp(beta_per_min * travelTime_min); // tnicolai: implement cost function as: Math.exp ( alpha * traveltime + beta * traveltime**2 + gamma * ln(traveltime) + delta * distance + epsilon * distance**2 + phi * ln(distance) ) 
			this.accessibilityTravelTimes += weight * costFunction;
		}
	}
	
	public void finalizeLogSum(double beta_per_min,
			Id originZoneID){
		
		// add in zone accessibility (same zone computation as above but using (minTravelTime / 2) as travel cost cost )
		if(numberOfWorkplacesPerZone.get( originZoneID ) != null){ // skipping zones no workplaces
			long weight = numberOfWorkplacesPerZone.get( originZoneID ).counter;
			double costFunction = Math.exp( beta_per_min * (this.minTravelTime / 2) );
			this.accessibilityTravelTimes += weight * costFunction;
		}
	}
	
	public double getLogSum(){
		double logSum = Math.log( this.accessibilityTravelTimes );
		return logSum;
	}
}