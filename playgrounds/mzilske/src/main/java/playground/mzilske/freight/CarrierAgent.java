package playground.mzilske.freight;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.mzilske.freight.api.Offer;

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
	
	abstract CarrierOffer requestOffer(Id linkId, Id linkId2, int shipmentSize, double startPickup, double endPickup, double startDelivery, double endDelivery);
	
	abstract void reset();

	abstract void informOfferRejected(Offer offer);

	abstract void informOfferAccepted(Contract contract);
	
	

}
