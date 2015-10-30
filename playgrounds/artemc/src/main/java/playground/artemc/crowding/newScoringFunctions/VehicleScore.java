package playground.artemc.crowding.newScoringFunctions;

import java.util.HashMap;
import java.util.Set;

import org.matsim.api.core.v01.Id;

/**
 * This class stores the externalities occuring in each vehicle of the schedule
 * 
 * Called by ScoreTracker
 * 
 *  @author grerat
 * 
 */

public class VehicleScore {

	private Id vehicleId;
	private HashMap<Id, Set<Id>> facilityId;
	private HashMap<Id, Double> facilityTime;
	private HashMap<Id, Double> dwellTime;
	private HashMap<Id, Double> vehicleCrowdingCost;
	private HashMap<Id, Double> vehicleCrowdednessExternalityCharge;
	private HashMap<Id, Double> vehicleInVehicleTimeDelayExternalityCharge;
	private HashMap<Id, Double> vehicleCapacityConstraintsExternalityCharge;
	private HashMap<Id, Double> vehicleMoneyPaid;
	
	public VehicleScore(Id vehicleId) {
		this.vehicleId = vehicleId;
		this.facilityId = new HashMap<Id, Set<Id>>();
		this.facilityTime = new HashMap<Id, Double>();
		this.dwellTime = new HashMap<Id, Double>();
		this.vehicleCrowdingCost = new HashMap<Id, Double>();
		this.vehicleCrowdednessExternalityCharge = new HashMap<Id, Double>();
		this.vehicleInVehicleTimeDelayExternalityCharge = new HashMap<Id, Double>();
		this.vehicleCapacityConstraintsExternalityCharge = new HashMap<Id, Double>();
		this.vehicleMoneyPaid = new HashMap<Id, Double>();
	}
	
	public Id getVehicleId() {
		return vehicleId;
	}

	public HashMap<Id, Set<Id>> getFacilityId() {
		return facilityId;
	}

	public void setFacilityId(HashMap<Id, Set<Id>> facilityId) {
		this.facilityId = facilityId;
	}
	
	public HashMap<Id, Double> getFacilityTime() {
		return facilityTime;
	}
	
	public HashMap<Id, Double> getDwellTime() {
		return dwellTime;
	}

	public void setDwellTime(HashMap<Id, Double> dwellTime) {
		this.dwellTime = dwellTime;
	}

	public void setFacilityTime(HashMap<Id, Double> facilityTime) {
		this.facilityTime = facilityTime;
	}

	public HashMap<Id, Double> getVehicleCrowdingCost() {
		return vehicleCrowdingCost;
	}

	public void setVehicleCrowdingCost(
			HashMap<Id, Double> VehicleCrowdingCost) {
		this.vehicleCrowdingCost = VehicleCrowdingCost;
	}

	public HashMap<Id, Double> getVehicleCrowdednessExternalityCharge() {
		return vehicleCrowdednessExternalityCharge;
	}

	public void setVehicleCrowdednessExternalityCharge(
			HashMap<Id, Double> vehicleCrowdednessExternalityCharge) {
		this.vehicleCrowdednessExternalityCharge = vehicleCrowdednessExternalityCharge;
	}

	public HashMap<Id, Double> getVehicleInVehicleTimeDelayExternalityCharge() {
		return vehicleInVehicleTimeDelayExternalityCharge;
	}

	public void setVehicleInVehicleTimeDelayExternalityCharge(
			HashMap<Id, Double> vehicleInVehicleTimeDelayExternalityCharge) {
		this.vehicleInVehicleTimeDelayExternalityCharge = vehicleInVehicleTimeDelayExternalityCharge;
	}

	public HashMap<Id, Double> getVehicleCapacityConstraintsExternalityCharge() {
		return vehicleCapacityConstraintsExternalityCharge;
	}

	public void setVehicleCapacityConstraintsExternalityCharge(
			HashMap<Id, Double> vehicleCapacityConstraintsExternalityCharge) {
		this.vehicleCapacityConstraintsExternalityCharge = vehicleCapacityConstraintsExternalityCharge;
	}

	public HashMap<Id, Double> getVehicleMoneyPaid() {
		return vehicleMoneyPaid;
	}

	public void setVehicleMoneyPaid(HashMap<Id, Double> VehicleMoneyPaid) {
		this.vehicleMoneyPaid = VehicleMoneyPaid;
	}

	
}
