package playground.sergioo.hits2012.stages;

import java.util.HashSet;
import java.util.Set;

public class MotorDriverStage extends MotorStage {

	public static final Set<String> PARK_TYPES = new HashSet<String>();
	public static final Set<String> OTHER_MODES = new HashSet<String>();
	
	private final double erpCost;
	private final boolean erpReimbursed;
	private final double parkCost;
	private final String parkType;
	private final boolean parkReimbursed;
	
	public MotorDriverStage(String id, String mode,	double walkTime,
			double inVehicleTime, double lastWalkTime, int numPassengers,
			double erpCost, boolean erpReimbursed, double parkCost,
			String parkType, boolean parkReimbursed) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, numPassengers);
		this.erpCost = erpCost;
		this.erpReimbursed = erpReimbursed;
		this.parkCost = parkCost;
		this.parkType = parkType;
		this.parkReimbursed = parkReimbursed;
	}
	
	public double getErpCost() {
		return erpCost;
	}
	
	public boolean isErpReimbursed() {
		return erpReimbursed;
	}
	
	public double getParkCost() {
		return parkCost;
	}
	
	public String getParkType() {
		return parkType;
	}
	
	public boolean isParkReimbursed() {
		return parkReimbursed;
	}

}
