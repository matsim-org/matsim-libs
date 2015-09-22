package playground.artemc.crowding.newScoringFunctions;

import org.matsim.api.core.v01.Id;
/**
 * This class stores for each agent the externalities he undergoes.
 * In addition to that, the trip duration, the facility of alighting and 
 * the vehicle are recorder
 * 
 * Called by ScoreTracker
 * 
 *  @author grerat
 * 
 */
public class PersonScore {

	private Id personId;
	private Id vehicleId;
	private double scoringTime;
	private double tripDuration;
	private Id facilityOfAlighting;

	private double totalUtility;
	private double crowdingUtility;
	private double travelUtility;
	private double activityUtility;
	private double waitingUtility;
	private double crowdednessExternalityCharge;
	private double inVehicleTimeDelayExternalityCharge;
	private double capacityConstraintsExternalityCharge;
	private double moneyPaid;
	
	public PersonScore(Id personId) {
		this.personId = personId;
		this.vehicleId = null;
		this.scoringTime = 0;
		this.facilityOfAlighting = null;
		this.tripDuration = 0;
		this.totalUtility = 0.0;
		this.crowdingUtility = 0.0;
		this.travelUtility = 0.0;
		this.crowdingUtility = 0.0;
		this.waitingUtility = 0.0;
		this.crowdednessExternalityCharge = 0.0;
		this.inVehicleTimeDelayExternalityCharge = 0.0;
		this.capacityConstraintsExternalityCharge = 0.0;
		this.moneyPaid = 0.0;
	}
	

	public Id getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(Id vehicleId) {
		this.vehicleId = vehicleId;
	}

	public double getScoringTime() {
		return scoringTime;
	}

	public void setScoringTime(double time) {
		this.scoringTime = time;
	}
	
	public double getTripDuration() {
		return tripDuration;
	}

	public void setTripDuration(double tripDuration) {
		this.tripDuration = tripDuration;
	}

	public Id getFacilityOfAlighting() {
		return facilityOfAlighting;
	}

	public void setFacilityOfAlighting(Id facilityOfAlighting) {
		this.facilityOfAlighting = facilityOfAlighting;
	}
	
	public double getTotalUtility() {
		return totalUtility;
	}


	public void setTotalUtility(double totalUtility) {
		this.totalUtility = totalUtility;
	}


	public double getCrowdingUtility() {
		return crowdingUtility;
	}


	public void setCrowdingUtility(double crowdingUtility) {
		this.crowdingUtility = crowdingUtility;
	}


	public double getTravelUtility() {
		return travelUtility;
	}


	public void setTravelUtility(double travelUtility) {
		this.travelUtility = travelUtility;
	}


	public double getActivityUtility() {
		return activityUtility;
	}


	public void setActivityUtility(double activityUtility) {
		this.activityUtility = activityUtility;
	}


	public double getWaitingUtility() {
		return waitingUtility;
	}


	public void setWaitingUtility(double waitingUtility) {
		this.waitingUtility = waitingUtility;
	}


	public Id getPersonId() {
		return personId;
	}

	public double getCrowdednessExternalityCharge() {
		return crowdednessExternalityCharge;
	}


	public void setCrowdednessExternalityCharge(double crowdednessExternalityCharge) {
		this.crowdednessExternalityCharge = crowdednessExternalityCharge;
	}


	public double getInVehicleTimeDelayExternalityCharge() {
		return inVehicleTimeDelayExternalityCharge;
	}


	public void setInVehicleTimeDelayExternalityCharge(
			double inVehicleTimeDelayExternalityCharge) {
		this.inVehicleTimeDelayExternalityCharge = inVehicleTimeDelayExternalityCharge;
	}


	public double getCapacityConstraintsExternalityCharge() {
		return capacityConstraintsExternalityCharge;
	}


	public void setCapacityConstraintsExternalityCharge(
			double capacityConstraintsExternalityCharge) {
		this.capacityConstraintsExternalityCharge = capacityConstraintsExternalityCharge;
	}

	public double getMoneyPaid() {
		return moneyPaid;
	}

	public void setMoneyPaid(double moneyPaid) {
		this.moneyPaid = moneyPaid;
	}

}
