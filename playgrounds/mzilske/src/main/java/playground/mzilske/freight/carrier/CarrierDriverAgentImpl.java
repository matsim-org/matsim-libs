package playground.mzilske.freight.carrier;

import org.matsim.api.core.v01.Id;

import playground.mzilske.freight.carrier.Tour.Pickup;
import playground.mzilske.freight.carrier.Tour.TourElement;

public class CarrierDriverAgentImpl implements CarrierDriverAgent{
	
	private int activityCounter = 0;
	
	private Id driverId;
	
	private double distance = 0.0;
	
	private double time = 0.0;
	
	private double startTime = 0.0;
	
	private int currentLoad = 0;
	
	private double performance = 0.0;
	
	private double volumes = 0.0;
	
	private double distanceRecordOfLastActivity = 0.0;

	private double additionalCosts = 0.0;
	
	private ScheduledTour scheduledTour;
	
	private CarrierAgent carrierAgent;

	CarrierDriverAgentImpl(CarrierAgent carrierAgent, Id driverId, ScheduledTour tour) {
		this.driverId = driverId;
		this.scheduledTour = tour;
		this.carrierAgent = carrierAgent;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#activityEndOccurs(java.lang.String, double)
	 */
	@Override
	public void activityEndOccurs(String activityType, double time) {
		Tour tour = this.scheduledTour.getTour();
		if (FreightConstants.START.equals(activityType)){
			startTime = time;
		}
		if (FreightConstants.PICKUP.equals(activityType)) {
			TourElement tourElement = tour.getTourElements().get(activityCounter);
			calculateLoadFactorComponent(tourElement);
			volumes += tourElement.getShipment().getSize();
			carrierAgent.notifyPickup(driverId, tourElement.getShipment(), time);
			activityCounter++;
		} else if (FreightConstants.DELIVERY.equals(activityType)) {
			TourElement tourElement = tour.getTourElements().get(activityCounter);
			calculateLoadFactorComponent(tourElement);
			carrierAgent.notifyDelivery(driverId, tourElement.getShipment(), time);
			activityCounter++;
		}
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#getCapacityUsage()
	 */
	@Override
	public double getCapacityUsage(){
		return performance / (distance*scheduledTour.getVehicle().getCapacity());
	}

	private void calculateLoadFactorComponent(TourElement tourElement) {
		performance += currentLoad*(distance-distanceRecordOfLastActivity);
		if(tourElement instanceof Pickup){
			currentLoad += tourElement.getShipment().getSize();
		}
		else{
			currentLoad -= tourElement.getShipment().getSize();
		}
		distanceRecordOfLastActivity = distance;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#activityStartOccurs(java.lang.String, double)
	 */
	@Override
	public void activityStartOccurs(String activityType, double time) {
		if(FreightConstants.END.equals(activityType)){
			time += time - startTime;
		}
		
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#tellDistance(double)
	 */
	@Override
	public void tellDistance(double distance) {
		this.distance += distance;
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#getDistance()
	 */
	@Override
	public double getDistance(){
		return distance;
	}

	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#tellTraveltime(double)
	 */
	@Override
	public void tellTraveltime(double time) {
		this.time += time;
	}

	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#tellToll(double)
	 */
	@Override
	public void tellToll(double toll) {
		this.additionalCosts  += toll;
	}
	
	/* (non-Javadoc)
	 * @see playground.mzilske.freight.CarrierDriverAgent#getAdditionalCosts()
	 */
	@Override
	public double getAdditionalCosts(){
		return this.additionalCosts;
	}

	@Override
	public double getVolumes() {
		return volumes;
	}

	@Override
	public double getPerformace() {
		return performance;
	}

	@Override
	public CarrierVehicle getVehicle() {
		return scheduledTour.getVehicle();
	}

	@Override
	public double getTime() {
		return time;
	}
	
	
	

}
