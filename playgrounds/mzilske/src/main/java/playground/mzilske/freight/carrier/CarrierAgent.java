package playground.mzilske.freight.carrier;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;


public interface CarrierAgent {
	
	abstract Id getId();
	
	abstract void calculateCosts();
	
	abstract void scoreSelectedPlan();
	
	abstract List<Plan> createFreightDriverPlans();
	
	abstract void activityStartOccurs(Id personId, String activityType, double time);
	
	abstract void activityEndOccurs(Id personId, String activityType, double time);
	
	abstract void tellDistance(Id personId, double distance);
	
	abstract void tellTraveltime(Id personId, double time);
	
	abstract void tellLink(Id personId, Id linkId);
	
	abstract void notifyPickup(Id driverId, Shipment shipment, double time);
	
	abstract void notifyDelivery(Id driverId, Shipment shipment, double time);
	
	abstract Collection<Id> getDriverIds();
	
	

}
