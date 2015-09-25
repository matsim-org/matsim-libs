package playground.artemc.crowding.newScoringFunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

/**
 * Store the sum of externalities produced in the whole day of simulation
 * In addition to that, call the personScore and VehicleExternalities class
 *   
 * @author grerat
 * 
 */

public class ScoreTracker {

	private double totalCrowdednessUtility;
	private double totalCrowdednessExternalityCharges;
	private double totalInVehicleTimeDelayExternalityCharges;
	private double totalCapacityConstraintsExternalityCharges;
	private double totalMoneyPaid;
	private HashMap<Id, PersonScore> personScores;
	private HashMap<Id, VehicleScore> vehicleExternalities;

	public ScoreTracker() {
		this.personScores = new HashMap<Id, PersonScore>();
		this.vehicleExternalities = new HashMap<Id, VehicleScore>();
	}
	
	public HashMap<Id, PersonScore> getPersonScores() {
		return personScores;
	}
	
	public HashMap<Id, VehicleScore> getVehicleExternalities() {
		return vehicleExternalities;
	}

	public double getTotalCrowdednessUtility() {
		return totalCrowdednessUtility;
	}

	public void setTotalCrowdednessUtility(double totalCrowdednessUtility) {
		this.totalCrowdednessUtility = totalCrowdednessUtility;
	}

	public double getTotalCrowdednessExternalityCharges() {
		return totalCrowdednessExternalityCharges;
	}

	public void setTotalCrowdednessExternalityCharges(double totalCrowdednessExternalityCharges) {
		this.totalCrowdednessExternalityCharges = totalCrowdednessExternalityCharges;
	}

	public double getTotalInVehicleTimeDelayExternalityCharges() {
		return totalInVehicleTimeDelayExternalityCharges;
	}

	public void setTotalInVehicleTimeDelayExternalityCharges(
			double totalInVehicleTimeDelayExternalityCharges) {
		this.totalInVehicleTimeDelayExternalityCharges = totalInVehicleTimeDelayExternalityCharges;
	}

	public double getTotalCapacityConstraintsExternalityCharges() {
		return totalCapacityConstraintsExternalityCharges;
	}

	public void setTotalCapacityConstraintsExternalityCharges(
			double totalCapacityConstraintsExternalityCharges) {
		this.totalCapacityConstraintsExternalityCharges = totalCapacityConstraintsExternalityCharges;
	}

	public double getTotalMoneyPaid() {
		return totalMoneyPaid;
	}

	public void setTotalMoneyPaid(double totalMoneyPaid) {
		this.totalMoneyPaid = totalMoneyPaid;
	}

}
