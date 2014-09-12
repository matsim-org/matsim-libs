package playground.sergioo.hits2012.stages;

import playground.sergioo.hits2012.Stage;

public class TaxiStage extends Stage {

	private final double waitTime;
	private final int numPassengers;
	private final double taxiFare;
	private final boolean taxiReimbursment;
	
	public TaxiStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime, int numPassengers,
			double taxiFare, boolean taxiReimbursment) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime);
		this.waitTime = waitTime;
		this.numPassengers = numPassengers;
		this.taxiFare = taxiFare;
		this.taxiReimbursment = taxiReimbursment;
	}

}
